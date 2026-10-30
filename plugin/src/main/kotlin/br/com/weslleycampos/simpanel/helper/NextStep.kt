package br.com.weslleycampos.simpanel.helper

import kotlin.time.Duration

/** What the panel does once a helper session ends. */
sealed interface NextStep {
    /** Starts a new helper for the same simulator [after] a pause. */
    data class Restart(val after: Duration) : NextStep

    /** Waits for a simulator to boot, as this one isn't any more. */
    data object AwaitBoot : NextStep

    /** Shows why, with Retry, as another helper would fail the same way. */
    data object GiveUp : NextStep
}
