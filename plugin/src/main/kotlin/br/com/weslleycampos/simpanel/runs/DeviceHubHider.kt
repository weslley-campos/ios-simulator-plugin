package br.com.weslleycampos.simpanel.runs

import br.com.weslleycampos.simpanel.simulators.DeviceHub
import br.com.weslleycampos.simpanel.simulators.Simctl
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val Log = logger<DeviceHubHider>()

/** The longest the hider keeps hiding DeviceHub after a run starts, long enough for a cold boot. */
private val LaunchWindow = 60.seconds

/** How often the hider looks for DeviceHub, which shows for about this long before it's hidden. */
private val HidePoll = 50.milliseconds

/** How often the hider checks whether the run's simulator has booted. */
private val BootCheck = 1.seconds

/**
 * Hides DeviceHub while runs launch on a simulator: their launcher opens it on the simulator, and
 * brings it to the front, every time.
 */
@Service(Service.Level.PROJECT)
class DeviceHubHider(private val scope: CoroutineScope) {
    /** By the run's execution id. */
    private val launching = ConcurrentHashMap<Long, Job>()

    /**
     * Hides DeviceHub each time it shows until the run [executionId] has launched on [udid], when
     * that's a simulator and not a device.
     */
    fun hideWhileLaunching(executionId: Long, udid: String) {
        val job = scope.launch(Dispatchers.IO) {
            val started = TimeSource.Monotonic.markNow()
            val watch = LaunchWatch()
            try {
                if (Simctl.list().none { it.udid == udid }) return@launch
                withTimeoutOrNull(LaunchWindow) {
                    val checks = launch { checkBoot(udid, watch, started) }
                    while (!watch.done(started.elapsedNow())) {
                        watch.polled(started.elapsedNow(), shown = DeviceHub.hide())
                        delay(HidePoll)
                    }
                    checks.cancel()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
            } finally {
                launching.remove(executionId, coroutineContext.job)
            }
        }
        launching.put(executionId, job)?.cancel()
    }

    /** Lets DeviceHub show again, as the run [executionId] stopped. */
    fun stop(executionId: Long) {
        launching.remove(executionId)?.cancel()
    }

    /** Lets DeviceHub show again for every run, as the user opens it. */
    fun stopAll() {
        launching.keys.forEach(::stop)
    }

    private suspend fun checkBoot(udid: String, watch: LaunchWatch, started: TimeMark) {
        try {
            while (true) {
                val at = started.elapsedNow()
                watch.checked(at, booted = Simctl.list().any { it.udid == udid && it.booted })
                delay(BootCheck)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.warn(e)
        }
    }
}
