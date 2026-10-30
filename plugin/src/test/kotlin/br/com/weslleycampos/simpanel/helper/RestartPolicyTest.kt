package br.com.weslleycampos.simpanel.helper

import br.com.weslleycampos.simpanel.protocol.HelperMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RestartPolicyTest {
    private fun failed(code: String) = SessionEnd.Failed(HelperMessage.Failure(code, "B849"))

    private val lost = SessionEnd.Lost(HelperExitedException(1))

    @Test
    fun restartsAHelperThatStoppedWithBackoffThenGivesUp() {
        val policy = RestartPolicy()

        val steps = List(4) { policy.next(lost, shown = Duration.ZERO) }

        assertEquals(
            listOf(
                NextStep.Restart(1.seconds),
                NextStep.Restart(2.seconds),
                NextStep.Restart(4.seconds),
                NextStep.GiveUp,
            ),
            steps,
        )
    }

    @Test
    fun restartsAfterAnInternalErrorAsAfterACrash() {
        assertEquals(NextStep.Restart(1.seconds), RestartPolicy().next(failed("internal"), shown = Duration.ZERO))
    }

    @Test
    fun givesTheRestartsBackAfterASteadySession() {
        val policy = RestartPolicy()
        repeat(3) { policy.next(lost, shown = Duration.ZERO) }

        assertEquals(NextStep.Restart(1.seconds), policy.next(lost, shown = 30.seconds))
    }

    @Test
    fun waitsForABootWhenTheSimulatorShutDown() {
        assertEquals(NextStep.AwaitBoot, RestartPolicy().next(failed("device_not_booted"), shown = 30.seconds))
    }

    @Test
    fun givesUpOnErrorsAnotherHelperWouldHitToo() {
        for (code in listOf("device_not_found", "hid_unavailable", "xcode_not_found", "unsupported_xcode")) {
            assertEquals(NextStep.GiveUp, RestartPolicy().next(failed(code), shown = Duration.ZERO), code)
        }
    }

    @Test
    fun waitsForAJustBootedSimulatorToDrawItsScreen() {
        val policy = RestartPolicy()

        val steps = List(20) { policy.next(failed("framebuffer_unavailable"), shown = Duration.ZERO) }

        assertEquals(List(19) { NextStep.Restart(1.seconds) } + NextStep.GiveUp, steps)
    }
}
