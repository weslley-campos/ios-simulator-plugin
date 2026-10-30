package br.com.weslleycampos.simpanel.keyboard

import java.text.Normalizer

private val Shift = listOf(HidUsage.LeftShift)
private val Option = listOf(HidUsage.LeftOption)
private val OptionShift = listOf(HidUsage.LeftOption, HidUsage.LeftShift)

/** What each key that isn't a letter types alone and with Shift. */
private val Symbols = mapOf(
    0x1e to "1!", 0x1f to "2@", 0x20 to "3#", 0x21 to "4$", 0x22 to "5%",
    0x23 to "6^", 0x24 to "7&", 0x25 to "8*", 0x26 to "9(", 0x27 to "0)",
    HidUsage.Space to " ", 0x2d to "-_", 0x2e to "=+", 0x2f to "[{", 0x30 to "]}",
    0x31 to "\\|", 0x33 to ";:", 0x34 to "'\"", 0x35 to "`~", 0x36 to ",<", 0x37 to ".>",
    0x38 to "/?",
)

/** What Option, then Option with Shift, types on the key that types the character alone. */
private val OptionLayer = mapOf(
    'a' to "åÅ", 'c' to "çÇ", 'g' to "©", 'm' to "µ", 'o' to "øØ", 'p' to "π", 'q' to "œŒ",
    'r' to "®", 's' to "ß", 'y' to "¥", '1' to "¡", '2' to "™€", '3' to "£", '4' to "¢",
    '6' to "§", '7' to "¶", '8' to "•°", '9' to "ª", '0' to "º", '-' to "–—", '=' to "≠±",
    '[' to "“”", ']' to "‘’", '\\' to "«»", ';' to "…", '\'' to "æÆ", ',' to "≤", '.' to "≥",
    '/' to "÷¿",
)

/** The letters each dead key puts its accent on, by the key's character alone. */
private val Accents = mapOf(
    '`' to "àèìòùÀÈÌÒÙ",
    'e' to "áéíóúÁÉÍÓÚ",
    'i' to "âêîôûÂÊÎÔÛ",
    'n' to "ãñõÃÑÕ",
    'u' to "äëïöüÿÄËÏÖÜŸ",
)

/**
 * How the simulator's hardware keyboard, on its US layout, types a character: with one key, or
 * as on a Mac, with Option and a key for an accent and then the letter it goes on.
 */
object UsLayout {
    private val basicKeys: Map<Char, Keystroke> = buildMap {
        for (letter in 'a'..'z') {
            put(letter, Keystroke(HidUsage.letter(letter)))
            put(letter.uppercaseChar(), Keystroke(HidUsage.letter(letter), Shift))
        }
        for ((usage, characters) in Symbols) {
            put(characters[0], Keystroke(usage))
            characters.getOrNull(1)?.let { put(it, Keystroke(usage, Shift)) }
        }
    }

    private val optionKeys: Map<Char, Keystroke> = buildMap {
        for ((key, characters) in OptionLayer) {
            val usage = basicKeys.getValue(key).usage
            put(characters[0], Keystroke(usage, Option))
            characters.getOrNull(1)?.let { put(it, Keystroke(usage, OptionShift)) }
        }
    }

    private val deadKeys: Map<Char, Keystroke> = buildMap {
        for ((key, letters) in Accents) {
            val deadKey = Keystroke(basicKeys.getValue(key).usage, Option)
            letters.forEach { put(it, deadKey) }
        }
    }

    /** The keystrokes that type [character], or null when the layout has no keys for it. */
    fun type(character: Char): List<Keystroke>? {
        (basicKeys[character] ?: optionKeys[character])?.let { return listOf(it) }
        val deadKey = deadKeys[character] ?: return null
        val letter = Normalizer.normalize(character.toString(), Normalizer.Form.NFD).first()
        return listOf(deadKey, basicKeys.getValue(letter))
    }
}
