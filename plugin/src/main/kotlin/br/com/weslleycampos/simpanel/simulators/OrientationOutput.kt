package br.com.weslleycampos.simpanel.simulators

import br.com.weslleycampos.simpanel.SimPanelJson

/**
 * The orientation in `xcrun devicectl device orientation` JSON output, leaving out lying flat, which
 * leaves the screen the way it was last held; portrait when devicectl names one the panel doesn't know.
 */
fun parseOrientation(json: String): Orientation {
    val name = SimPanelJson.decodeFromString<OrientationOutputDto>(json).result.deviceOrientationNonFlat
    return Orientation.entries.firstOrNull { it.devicectlName == name } ?: Orientation.Portrait
}
