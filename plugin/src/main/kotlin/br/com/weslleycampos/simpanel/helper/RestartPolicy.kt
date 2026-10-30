package br.com.weslleycampos.simpanel.helper

import br.com.weslleycampos.simpanel.protocol.ErrorCode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** A session that showed the simulator this long earns its restarts back. */
private val SteadySession = 10.seconds

/** A simulator that has just booted draws its screen within this many tries this far apart. */
private const val ScreenTries = 20
private val ScreenRetry = 1.seconds

/** The pauses before each restart of a helper that stopped for another reason. */
private val Backoff = listOf(1.seconds, 2.seconds, 4.seconds)

/** Errors that a new helper would hit again. */
private val Lasting = setOf(
    ErrorCode.DeviceNotFound,
    ErrorCode.HidUnavailable,
    ErrorCode.XcodeNotFound,
    ErrorCode.UnsupportedXcode,
)

/** Decides what follows each helper session that ended on its own, for one simulator. */
class RestartPolicy {
    private var restarts = 0
    private var screenTries = 0

    /** [shown] is how long the session showed the simulator, zero when it never attached. */
    fun next(end: SessionEnd, shown: Duration): NextStep {
        if (shown >= SteadySession) {
            restarts = 0
            screenTries = 0
        }
        val code = (end as? SessionEnd.Failed)?.failure?.errorCode
        return when {
            code == ErrorCode.DeviceNotBooted -> NextStep.AwaitBoot
            code in Lasting -> NextStep.GiveUp
            code == ErrorCode.FramebufferUnavailable ->
                if (++screenTries < ScreenTries) NextStep.Restart(ScreenRetry) else NextStep.GiveUp
            else -> Backoff.getOrNull(restarts++)?.let(NextStep::Restart) ?: NextStep.GiveUp
        }
    }
}
