package br.com.weslleycampos.simpanel.simulators

/**
 * The simulator the panel shows: the one shown last while it's booted, else the first booted one.
 * With none booted, the one shown last, or else the first, which the panel offers to boot.
 */
fun pickSimulator(simulators: List<Simulator>, lastUdid: String?): Simulator? {
    val last = simulators.firstOrNull { it.udid == lastUdid }
    return last?.takeIf { it.booted } ?: simulators.firstOrNull { it.booted } ?: last ?: simulators.firstOrNull()
}
