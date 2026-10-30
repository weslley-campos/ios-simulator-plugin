package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.protocol.ErrorCode

internal fun ErrorCode.text(): String = when (this) {
    ErrorCode.DeviceNotFound -> message("error.deviceNotFound")
    ErrorCode.DeviceNotBooted -> message("error.deviceNotBooted")
    ErrorCode.FramebufferUnavailable -> message("error.framebufferUnavailable")
    ErrorCode.HidUnavailable -> message("error.hidUnavailable")
    ErrorCode.XcodeNotFound -> message("error.xcodeNotFound")
    ErrorCode.UnsupportedXcode -> message("error.unsupportedXcode")
    ErrorCode.Internal -> message("error.internal")
}
