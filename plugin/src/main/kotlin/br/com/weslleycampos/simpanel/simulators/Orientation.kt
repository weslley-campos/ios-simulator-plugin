package br.com.weslleycampos.simpanel.simulators

/** Which way up the simulator is held, and so how many quarter turns to the left its screen is shown at. */
enum class Orientation(val quarterTurnsLeft: Int, internal val devicectlName: String) {
    Portrait(0, "portrait"),
    LandscapeLeft(1, "landscapeLeft"),
    PortraitUpsideDown(2, "portraitUpsideDown"),
    LandscapeRight(3, "landscapeRight"),
    ;

    /** Lying on a side, with the screen's width and height swapped. */
    val sideways get() = quarterTurnsLeft % 2 == 1
}
