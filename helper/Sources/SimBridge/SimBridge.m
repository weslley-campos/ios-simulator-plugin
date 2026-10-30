#import "SimBridge.h"

#import <AppKit/NSEvent.h>
#import <dlfcn.h>
#import <mach/message.h>

NSErrorDomain const SPErrorDomain = @"SimPanelErrorDomain";

static NSString *const CoreSimulatorPath =
    @"/Library/Developer/PrivateFrameworks/CoreSimulator.framework/CoreSimulator";

// Relative to the developer directory: Xcode 27 moved SimulatorKit to SharedFrameworks.
static NSArray<NSString *> *SimulatorKitPaths(void) {
    return @[
        @"../SharedFrameworks/SimulatorKit.framework/SimulatorKit",
        @"Library/PrivateFrameworks/SimulatorKit.framework/SimulatorKit",
    ];
}

static const unsigned long long SimDeviceStateBooted = 3;

// IndigoHID values, read from SimulatorKit 27.1's message builders.
static const uint32_t IndigoTargetDigitizer = 0x32;
static const uint32_t IndigoTargetButton = 0x33;
static const uint32_t IndigoDirectionDown = 1;
static const uint32_t IndigoDirectionUp = 2;
static const uint32_t IndigoEdgeNone = 0;
static const uint32_t IndigoButtonHome = 0x0;
static const uint32_t IndigoButtonLock = 0x1;
static const uint32_t IndigoButtonSiri = 0x400002;

// An Indigo message is a mach header, then a payload stride at 0x18 and a payload count at
// 0x1c, then the payloads from 0x20. The builders leave msgh_size unset; the client reads it.
static const size_t IndigoStrideOffset = 0x18;
static const size_t IndigoCountOffset = 0x1c;
static const size_t IndigoPayloadsOffset = 0x20;

typedef void *_Nullable (*IndigoMouseFn)(CGPoint *point, CGPoint *_Nullable second,
                                         uint32_t target, NSUInteger eventType, CGSize size,
                                         NSUInteger edge);
typedef void *_Nullable (*IndigoButtonFn)(uint32_t source, uint32_t direction, uint32_t target);
typedef void *_Nullable (*IndigoKeyboardFn)(uint32_t usage, uint32_t direction);
typedef void *_Nullable (*IndigoArbitraryFn)(uint32_t target, uint32_t page, uint32_t usage,
                                             uint32_t direction);

@protocol SPServiceContextClass <NSObject>
+ (nullable id)sharedServiceContextForDeveloperDir:(NSString *)developerDir
                                             error:(NSError **)error;
@end

@protocol SPServiceContext <NSObject>
- (nullable id)defaultDeviceSetWithError:(NSError **)error;
@end

@protocol SPLegacyHIDClient <NSObject>
- (nullable instancetype)initWithDevice:(id)device error:(NSError **)error;
- (void)sendWithMessage:(void *)message
           freeWhenDone:(BOOL)freeWhenDone
        completionQueue:(nullable dispatch_queue_t)queue
             completion:(nullable void (^)(NSError *_Nullable error))completion;
@end

// The IO ports are ROCKit proxies, which forward messages but don't answer key-value coding.
@protocol SPIOPort <NSObject>
- (id)descriptor;
@end

@protocol SPDisplayDescriptor <NSObject>
- (id)state;
@optional
- (nullable IOSurface *)framebufferSurface;
- (nullable IOSurface *)ioSurface;
@end

@protocol SPDisplayState <NSObject>
- (unsigned short)displayClass;
@end

static NSError *SPMakeError(SPError code, NSString *message) {
    return [NSError errorWithDomain:SPErrorDomain
                               code:code
                           userInfo:@{NSLocalizedDescriptionKey: message}];
}

@interface SPSimulator ()
@property (nonatomic, strong) id device;
@property (nonatomic, strong, nullable) id<SPDisplayDescriptor> display;
@property (nonatomic, strong, nullable) id<SPLegacyHIDClient> hid;
@property (nonatomic, strong) dispatch_queue_t hidQueue;
@property (nonatomic, assign) void *simulatorKit;
@property (nonatomic, assign) IndigoMouseFn mouseMessage;
@property (nonatomic, assign) IndigoButtonFn buttonMessage;
@property (nonatomic, assign) IndigoKeyboardFn keyboardMessage;
@property (nonatomic, assign) IndigoArbitraryFn arbitraryMessage;
@end

@implementation SPSimulator

+ (nullable instancetype)simulatorWithUDID:(NSString *)udid
                              developerDir:(NSString *)developerDir
                                     error:(NSError **)error {
    if (![NSFileManager.defaultManager fileExistsAtPath:developerDir]) {
        if (error) *error = SPMakeError(SPErrorXcodeNotFound, developerDir);
        return nil;
    }
    if (!dlopen(CoreSimulatorPath.fileSystemRepresentation, RTLD_NOW)) {
        if (error) *error = SPMakeError(SPErrorUnsupportedXcode, @(dlerror()));
        return nil;
    }
    void *simulatorKit = NULL;
    for (NSString *path in SimulatorKitPaths()) {
        NSString *full = [developerDir stringByAppendingPathComponent:path].stringByStandardizingPath;
        simulatorKit = dlopen(full.fileSystemRepresentation, RTLD_NOW);
        if (simulatorKit) break;
    }
    if (!simulatorKit) {
        if (error) *error = SPMakeError(SPErrorUnsupportedXcode, @"SimulatorKit not found");
        return nil;
    }

    Class<SPServiceContextClass> contextClass = NSClassFromString(@"SimServiceContext");
    if (![contextClass respondsToSelector:@selector(sharedServiceContextForDeveloperDir:error:)]) {
        if (error) *error = SPMakeError(SPErrorUnsupportedXcode, @"SimServiceContext missing");
        return nil;
    }
    id<SPServiceContext> context =
        [contextClass sharedServiceContextForDeveloperDir:developerDir error:error];
    id deviceSet = [context defaultDeviceSetWithError:error];
    if (!deviceSet) return nil;

    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:udid];
    NSDictionary *devices = [deviceSet valueForKey:@"devicesByUDID"];
    id device = uuid ? devices[uuid] : nil;
    if (!device) {
        if (error) *error = SPMakeError(SPErrorDeviceNotFound, udid);
        return nil;
    }
    if ([[device valueForKey:@"state"] unsignedLongLongValue] != SimDeviceStateBooted) {
        if (error) *error = SPMakeError(SPErrorDeviceNotBooted, udid);
        return nil;
    }

    SPSimulator *simulator = [[SPSimulator alloc] initWithDevice:device simulatorKit:simulatorKit];
    if (!simulator.display) {
        if (error) *error = SPMakeError(SPErrorFramebufferUnavailable, @"No main display port");
        return nil;
    }
    return simulator;
}

- (instancetype)initWithDevice:(id)device simulatorKit:(void *)simulatorKit {
    self = [super init];
    if (self) {
        _device = device;
        _simulatorKit = simulatorKit;
        _udid = [[device valueForKey:@"UDID"] UUIDString];
        _name = [[device valueForKey:@"name"] copy];
        _display = [SPSimulator mainDisplayOfDevice:device];
        _hidQueue = dispatch_queue_create("simpanel.hid", DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

+ (nullable id<SPDisplayDescriptor>)mainDisplayOfDevice:(id)device {
    NSArray<id<SPIOPort>> *ports = [[device valueForKey:@"io"] valueForKey:@"ioPorts"];
    for (id<SPIOPort> port in ports) {
        if (![port respondsToSelector:@selector(descriptor)]) continue;
        id<SPDisplayDescriptor> descriptor = [port descriptor];
        BOOL renderable = [descriptor respondsToSelector:@selector(framebufferSurface)]
            || [descriptor respondsToSelector:@selector(ioSurface)];
        if (!renderable || ![descriptor respondsToSelector:@selector(state)]) continue;
        id<SPDisplayState> state = [descriptor state];
        if (![state respondsToSelector:@selector(displayClass)] || [state displayClass] != 0) {
            continue;
        }
        return descriptor;
    }
    return nil;
}

- (nullable IOSurface *)framebuffer {
    if ([self.display respondsToSelector:@selector(framebufferSurface)]) {
        return [self.display framebufferSurface];
    }
    return [self.display ioSurface];
}

- (BOOL)connectInputWithError:(NSError **)error {
    if (self.hid) return YES;
    self.mouseMessage = (IndigoMouseFn)dlsym(self.simulatorKit, "IndigoHIDMessageForMouseNSEvent");
    self.buttonMessage = (IndigoButtonFn)dlsym(self.simulatorKit, "IndigoHIDMessageForButton");
    self.keyboardMessage =
        (IndigoKeyboardFn)dlsym(self.simulatorKit, "IndigoHIDMessageForKeyboardArbitrary");
    self.arbitraryMessage =
        (IndigoArbitraryFn)dlsym(self.simulatorKit, "IndigoHIDMessageForHIDArbitrary");
    Class clientClass = NSClassFromString(@"SimulatorKit.SimDeviceLegacyHIDClient")
        ?: NSClassFromString(@"_TtC12SimulatorKit24SimDeviceLegacyHIDClient");
    if (!self.mouseMessage || !self.buttonMessage || !self.keyboardMessage
        || !self.arbitraryMessage || !clientClass) {
        if (error) *error = SPMakeError(SPErrorHIDUnavailable, @"IndigoHID symbols missing");
        return NO;
    }
    NSError *clientError = nil;
    id<SPLegacyHIDClient> client =
        [(id<SPLegacyHIDClient>)[clientClass alloc] initWithDevice:self.device error:&clientError];
    if (!client) {
        if (error) {
            *error = SPMakeError(SPErrorHIDUnavailable,
                                 clientError.localizedDescription ?: @"HID client failed");
        }
        return NO;
    }
    self.hid = client;
    return YES;
}

- (BOOL)touch:(SPTouchPhase)phase x:(double)x y:(double)y {
    NSEventType type = NSEventTypeLeftMouseDown;
    if (phase == SPTouchPhaseMove) type = NSEventTypeLeftMouseDragged;
    if (phase == SPTouchPhaseUp) type = NSEventTypeLeftMouseUp;
    CGPoint point = CGPointMake(x, y);
    void *message = self.mouseMessage(&point, NULL, IndigoTargetDigitizer, type,
                                      CGSizeMake(1, 1), IndigoEdgeNone);
    if (!message) return NO;
    [self send:message];
    return YES;
}

- (void)button:(SPButton)button down:(BOOL)down {
    uint32_t source = IndigoButtonHome;
    if (button == SPButtonLock) source = IndigoButtonLock;
    if (button == SPButtonSiri) source = IndigoButtonSiri;
    uint32_t direction = down ? IndigoDirectionDown : IndigoDirectionUp;
    [self send:self.buttonMessage(source, direction, IndigoTargetButton)];
}

- (void)usagePage:(uint32_t)page usage:(uint32_t)usage down:(BOOL)down {
    uint32_t direction = down ? IndigoDirectionDown : IndigoDirectionUp;
    // Arbitrary usages reach the device only through the digitizer target; the button target
    // drops them without an error.
    [self send:self.arbitraryMessage(IndigoTargetDigitizer, page, usage, direction)];
}

- (void)key:(uint32_t)usage down:(BOOL)down {
    [self send:self.keyboardMessage(usage, down ? IndigoDirectionDown : IndigoDirectionUp)];
}

- (void)send:(nullable void *)message {
    if (!message || !self.hid) {
        free(message);
        return;
    }
    uint8_t *bytes = message;
    uint32_t stride = *(uint32_t *)(bytes + IndigoStrideOffset);
    uint8_t count = bytes[IndigoCountOffset];
    ((mach_msg_header_t *)message)->msgh_size =
        (mach_msg_size_t)(IndigoPayloadsOffset + stride * count);
    [self.hid sendWithMessage:message
                 freeWhenDone:YES
              completionQueue:self.hidQueue
                   completion:^(NSError *error) {
        if (error) NSLog(@"HID send failed: %@", error);
    }];
}

@end
