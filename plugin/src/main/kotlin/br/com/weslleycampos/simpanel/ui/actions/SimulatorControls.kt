package br.com.weslleycampos.simpanel.ui.actions

import br.com.weslleycampos.simpanel.protocol.DeviceButton
import br.com.weslleycampos.simpanel.simulators.Rotation

/** What the toolbar's actions do to the simulator the panel shows. */
interface SimulatorControls {
    /** True from the helper's hello until its session ends. */
    val attached: Boolean

    /** Presses and releases each of [buttons] in turn, so two Home presses open the app switcher. */
    fun press(vararg buttons: DeviceButton)

    /** Turns the simulator a quarter turn, and the screen with it. */
    fun rotate(rotation: Rotation)

    /** Saves a screenshot where Simulator saves them and copies it to the clipboard. */
    fun takeScreenshot()

    /** Pastes the Mac clipboard's text where the simulator's cursor is, leaving it on its pasteboard. */
    fun paste()

    /** Shuts the simulator down, leaving a Boot link in its place. */
    fun shutDown()

    /** Draws the device around the screen, as Simulator does. */
    var frameVisible: Boolean

    /**
     * Shows the simulator's on-screen keyboard while a text field has focus; hidden, the simulator
     * acts as though a hardware keyboard were connected. The Mac keyboard types either way.
     */
    var softwareKeyboardShown: Boolean

    /** Paints how many frames arrived in the last second over the screen. */
    var frameRateVisible: Boolean
}
