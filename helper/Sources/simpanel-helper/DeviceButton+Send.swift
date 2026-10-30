import SimBridge
import SimPanelKit

private let ConsumerPage: UInt32 = 12
private let TelephonyPage: UInt32 = 11
private let VolumeUpUsage: UInt32 = 233
private let VolumeDownUsage: UInt32 = 234
private let ActionUsage: UInt32 = 45

extension DeviceButton {
    /// Home, power and Siri have IndigoHID builders of their own; the rest go by the HID usage
    /// the device chrome lists for them.
    func send(down: Bool, to simulator: SPSimulator) {
        switch self {
        case .home: simulator.button(.home, down: down)
        case .power: simulator.button(.lock, down: down)
        case .siri: simulator.button(.siri, down: down)
        case .volumeUp: simulator.usagePage(ConsumerPage, usage: VolumeUpUsage, down: down)
        case .volumeDown: simulator.usagePage(ConsumerPage, usage: VolumeDownUsage, down: down)
        case .action: simulator.usagePage(TelephonyPage, usage: ActionUsage, down: down)
        }
    }
}
