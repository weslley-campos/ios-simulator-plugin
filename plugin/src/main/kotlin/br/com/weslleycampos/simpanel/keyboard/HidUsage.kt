package br.com.weslleycampos.simpanel.keyboard

/** The keys of the USB HID keyboard page that the simulator's hardware keyboard takes. */
object HidUsage {
    const val Enter = 0x28
    const val Escape = 0x29
    const val Backspace = 0x2a
    const val Tab = 0x2b
    const val Space = 0x2c
    const val Home = 0x4a
    const val PageUp = 0x4b
    const val DeleteForward = 0x4c
    const val End = 0x4d
    const val PageDown = 0x4e
    const val Right = 0x4f
    const val Left = 0x50
    const val Down = 0x51
    const val Up = 0x52
    const val LeftShift = 0xe1
    const val LeftOption = 0xe2
    const val LeftCommand = 0xe3

    /** The key of a lowercase letter, a to z. */
    fun letter(letter: Char): Int = 0x04 + (letter - 'a')
}
