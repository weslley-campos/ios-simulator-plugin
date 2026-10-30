#import <Foundation/Foundation.h>
#import <IOSurface/IOSurfaceObjC.h>

NS_ASSUME_NONNULL_BEGIN

extern NSErrorDomain const SPErrorDomain;

typedef NS_ERROR_ENUM(SPErrorDomain, SPError) {
    SPErrorXcodeNotFound = 1,
    SPErrorUnsupportedXcode = 2,
    SPErrorDeviceNotFound = 3,
    SPErrorDeviceNotBooted = 4,
    SPErrorFramebufferUnavailable = 5,
    SPErrorHIDUnavailable = 6,
};

typedef NS_ENUM(NSInteger, SPTouchPhase) {
    SPTouchPhaseDown,
    SPTouchPhaseMove,
    SPTouchPhaseUp,
};

typedef NS_ENUM(NSInteger, SPButton) {
    SPButtonHome,
    SPButtonLock,
    SPButtonSiri,
};

/// A booted simulator, reached through CoreSimulator and SimulatorKit, which are loaded at
/// runtime so that a missing private symbol is an error instead of a crash at launch.
@interface SPSimulator : NSObject

- (instancetype)init NS_UNAVAILABLE;

+ (nullable instancetype)simulatorWithUDID:(NSString *)udid
                              developerDir:(NSString *)developerDir
                                     error:(NSError **)error;

@property (nonatomic, readonly, copy) NSString *udid;
@property (nonatomic, readonly, copy) NSString *name;

/// The device type's bundle, which names the device chrome and holds the screen's mask.
@property (nonatomic, readonly, copy, nullable) NSString *deviceTypePath;

/// Whether CoreSimulator still has the device booted; it stops being once the device shuts down.
@property (nonatomic, readonly, getter=isBooted) BOOL booted;

/// Pixels per point on the main screen.
@property (nonatomic, readonly) double screenScale;

/// The main display's current surface; it is replaced when the display changes size.
@property (nonatomic, readonly, nullable) IOSurface *framebuffer;

- (BOOL)connectInputWithError:(NSError **)error;

/// Sends a touch at a point given as fractions of the screen. Returns NO when SimulatorKit
/// drops a move that comes less than a frame after the previous one.
- (BOOL)touch:(SPTouchPhase)phase x:(double)x y:(double)y;

- (void)button:(SPButton)button down:(BOOL)down;

/// Presses a button by its HID usage page and usage, as the device chrome lists them.
- (void)usagePage:(uint32_t)page usage:(uint32_t)usage down:(BOOL)down;

/// Sends a key by its HID keyboard usage code.
- (void)key:(uint32_t)usage down:(BOOL)down;

@end

NS_ASSUME_NONNULL_END
