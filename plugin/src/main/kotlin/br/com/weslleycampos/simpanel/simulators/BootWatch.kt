package br.com.weslleycampos.simpanel.simulators

/** What the panel does with the simulators a poll lists while it waits for one to boot. */
sealed interface BootWatch {
    /** Shows [simulator], which has booted. */
    data class Show(val simulator: Simulator) : BootWatch

    /** Waits for [simulator] instead, as the one waited for is gone; null when there's none. */
    data class WaitFor(val simulator: Simulator?) : BootWatch

    data object KeepWaiting : BootWatch

    companion object {
        /**
         * Shows [waitingFor] once it boots, or else a simulator that booted since the last poll found
         * [bootedBefore] booted, which is null on the first poll. A simulator that was booted all along
         * doesn't take the place of the one the panel waits for.
         */
        fun after(simulators: List<Simulator>, waitingFor: Simulator?, bootedBefore: Set<String>?): BootWatch {
            val booted = simulators.filter { it.booted }
            val next = booted.firstOrNull { it.udid == waitingFor?.udid }
                ?: booted.firstOrNull { bootedBefore != null && it.udid !in bootedBefore }
            val waited = simulators.any { it.udid == waitingFor?.udid }
            return when {
                next != null -> Show(next)
                waited || (waitingFor == null && simulators.isEmpty()) -> KeepWaiting
                else -> {
                    val pick = pickSimulator(simulators, lastUdid = null)
                    if (pick?.booted == true) Show(pick) else WaitFor(pick)
                }
            }
        }
    }
}
