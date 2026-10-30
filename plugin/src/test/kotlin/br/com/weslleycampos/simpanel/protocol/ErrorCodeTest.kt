package br.com.weslleycampos.simpanel.protocol

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorCodeTest {
    @ParameterizedTest
    @CsvSource(
        "device_not_found, DeviceNotFound",
        "device_not_booted, DeviceNotBooted",
        "framebuffer_unavailable, FramebufferUnavailable",
        "hid_unavailable, HidUnavailable",
        "xcode_not_found, XcodeNotFound",
        "unsupported_xcode, UnsupportedXcode",
        "internal, Internal",
    )
    fun mapsEveryCodeTheHelperSends(wire: String, expected: ErrorCode) {
        assertEquals(expected, ErrorCode.of(wire))
    }

    @Test
    fun mapsAnUnknownCodeToInternal() {
        assertEquals(ErrorCode.Internal, ErrorCode.of("disk_on_fire"))
    }
}
