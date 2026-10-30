package br.com.weslleycampos.simpanel.protocol

enum class ErrorCode(val wire: String) {
    DeviceNotFound("device_not_found"),
    DeviceNotBooted("device_not_booted"),
    FramebufferUnavailable("framebuffer_unavailable"),
    HidUnavailable("hid_unavailable"),
    XcodeNotFound("xcode_not_found"),
    UnsupportedXcode("unsupported_xcode"),
    Internal("internal");

    companion object {
        /** [Internal] for a code a newer helper sends that this plugin doesn't know. */
        fun of(wire: String): ErrorCode = entries.firstOrNull { it.wire == wire } ?: Internal
    }
}
