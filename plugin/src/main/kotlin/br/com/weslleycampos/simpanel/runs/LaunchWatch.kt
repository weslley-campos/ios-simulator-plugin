package br.com.weslleycampos.simpanel.runs

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How long DeviceHub stays hidden after the launcher last showed it before the launch is done. */
private val Settle = 5.seconds

/**
 * Tells when a run's launcher is done showing DeviceHub, from the polls that hide it and the checks
 * of the run's simulator, each timed from the run's start. The launcher boots the simulator and only
 * then opens DeviceHub on it, so a show counts only once no check started after it found the
 * simulator shut down. A show at the first poll is DeviceHub left open before the run, which counts
 * neither. Read and written from more than one thread.
 */
class LaunchWatch {
    private var polled = false
    private var booted = false
    private var shownAt: Duration? = null

    /** Records a check started at [at] that found the simulator [booted] or not. */
    @Synchronized
    fun checked(at: Duration, booted: Boolean) {
        this.booted = booted
        if (!booted && shownAt?.let { it <= at } == true) shownAt = null
    }

    /** Records a poll at [at] that found DeviceHub [shown], and hid it. */
    @Synchronized
    fun polled(at: Duration, shown: Boolean) {
        if (shown && polled) shownAt = at
        polled = true
    }

    /** Whether the launch is done showing DeviceHub at [at]. */
    @Synchronized
    fun done(at: Duration): Boolean {
        val shown = shownAt ?: return false
        return booted && at - shown >= Settle
    }
}
