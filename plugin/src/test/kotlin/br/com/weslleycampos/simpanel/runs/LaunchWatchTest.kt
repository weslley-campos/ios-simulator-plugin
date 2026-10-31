package br.com.weslleycampos.simpanel.runs

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LaunchWatchTest {
    @Test
    fun isDoneFiveSecondsAfterTheLauncherShowsDeviceHubOnABootedSimulator() {
        val watch = LaunchWatch()
        watch.checked(0.seconds, booted = true)
        watch.polled(0.seconds, shown = false)

        watch.polled(1.seconds, shown = true)

        assertFalse(watch.done(5900.milliseconds))
        assertTrue(watch.done(6.seconds))
    }

    @Test
    fun waitsAgainEachTimeDeviceHubShowsAgain() {
        val watch = LaunchWatch()
        watch.checked(0.seconds, booted = true)
        watch.polled(0.seconds, shown = false)

        watch.polled(1.seconds, shown = true)
        watch.polled(2500.milliseconds, shown = true)

        assertFalse(watch.done(6.seconds))
        assertTrue(watch.done(7500.milliseconds))
    }

    @Test
    fun ignoresDeviceHubLeftOpenBeforeTheRun() {
        val watch = LaunchWatch()
        watch.checked(0.seconds, booted = true)

        watch.polled(0.seconds, shown = true)

        assertFalse(watch.done(30.seconds))
    }

    @Test
    fun ignoresShowsWhileTheSimulatorBoots() {
        val watch = LaunchWatch()
        watch.polled(0.seconds, shown = false)
        watch.checked(0.seconds, booted = false)

        watch.polled(3.seconds, shown = true)
        watch.checked(20.seconds, booted = false)

        assertFalse(watch.done(30.seconds))
        watch.polled(20500.milliseconds, shown = true)
        watch.checked(21.seconds, booted = true)
        assertFalse(watch.done(25400.milliseconds))
        assertTrue(watch.done(25500.milliseconds))
    }

    @Test
    fun countsAShowThatCameAfterTheCheckThatLastFoundTheSimulatorShutDown() {
        val watch = LaunchWatch()
        watch.polled(0.seconds, shown = false)

        watch.polled(20300.milliseconds, shown = true)
        watch.checked(20.seconds, booted = false)
        watch.checked(21.seconds, booted = true)

        assertTrue(watch.done(25300.milliseconds))
    }

    @Test
    fun isNotDoneBeforeACheckFindsTheSimulatorBooted() {
        val watch = LaunchWatch()
        watch.polled(0.seconds, shown = false)

        watch.polled(1.seconds, shown = true)

        assertFalse(watch.done(30.seconds))
    }
}
