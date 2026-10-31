package br.com.weslleycampos.simpanel.ui

import com.intellij.openapi.util.IconLoader

/**
 * Running Devices' toolbar icons, copied from Android Studio's artwork under Apache 2.0, and the
 * IntelliJ Platform's keyboard icon, under the same license.
 */
object ControlIcons {
    val Power = icon("power")
    val VolumeUp = icon("volume-up")
    val VolumeDown = icon("volume-down")
    val RotateLeft = icon("rotate-left")
    val RotateRight = icon("rotate-right")
    val Home = icon("home")
    val AppSwitcher = icon("overview")
    val Screenshot = icon("screenshot")
    val DeviceFrame = icon("device-frame")
    val Keyboard = icon("keyboard")

    private fun icon(name: String) = IconLoader.getIcon("/icons/controls/$name.svg", ControlIcons::class.java)
}
