package br.com.weslleycampos.simpanel.helper

import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.protocol.TouchPhase
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val SettingsBundle = "com.apple.Preferences"

/**
 * Drives the real helper against a booted simulator: `SIMPANEL_UDID=<udid> ./gradlew test`.
 * It relaunches Settings there, so the swipe always has a list to scroll.
 */
@EnabledIfEnvironmentVariable(named = "SIMPANEL_UDID", matches = ".+")
class HelperSessionLiveTest {
    private val udid = System.getenv("SIMPANEL_UDID")

    private val helper = Path.of(System.getProperty("simpanel.helper"))

    @Test
    fun streamsDecodedFramesWhileASwipeScrolls(): Unit = runBlocking {
        openSettings()
        val session = HelperSession(helper, udid)
        val start = System.nanoTime()
        val events = mutableListOf<Pair<Double, HelperEvent>>()
        session.send(PluginMessage.Viewport(400, 870))
        val running = launch { session.run { events += (System.nanoTime() - start) / 1e9 to it } }

        delay(1.seconds)
        val swipeStart = (System.nanoTime() - start) / 1e9
        session.swipe()
        delay(1500.milliseconds)
        running.cancelAndJoin()

        val attached = assertIs<HelperEvent.Attached>(events.first().second)
        val frames = events.filter { it.second is HelperEvent.FrameReady && it.first >= swipeStart }.map { it.first }
        assertTrue(frames.size >= 2, "the swipe scrolled the screen")
        val fps = (frames.size - 1) / (frames.last() - frames.first())
        val image = (events.last { it.second is HelperEvent.FrameReady }.second as HelperEvent.FrameReady).image
        println(
            "attached to ${attached.hello.name}; ${frames.size} frames over " +
                "%.2f s while scrolling, %.1f fps, last frame %d x %d".format(
                    frames.last() - frames.first(), fps, image.width, image.height,
                ),
        )
        assertTrue(image.height <= 870 && image.width <= 400, "frames fit the viewport")
        assertTrue(fps >= 25, "decoded frames keep up with the helper while scrolling")
    }

    @Test
    fun capsTheFrameRateAtTheSettingsMaxFps(): Unit = runBlocking {
        openSettings()
        val session = HelperSession(helper, udid)
        val start = System.nanoTime()
        val frames = mutableListOf<Double>()
        session.send(PluginMessage.Viewport(400, 870))
        session.send(PluginMessage.Settings(maxFps = 5, jpegQuality = 0.5))
        val running = launch {
            session.run { if (it is HelperEvent.FrameReady) frames += (System.nanoTime() - start) / 1e9 }
        }

        delay(1.seconds)
        val swipeStart = (System.nanoTime() - start) / 1e9
        session.swipe()
        delay(1500.milliseconds)
        running.cancelAndJoin()

        val scrolling = frames.filter { it >= swipeStart }
        assertTrue(scrolling.size >= 2, "the swipe scrolled the screen")
        val fps = (scrolling.size - 1) / (scrolling.last() - scrolling.first())
        println("%.1f fps while scrolling at maxFps 5".format(fps))
        assertTrue(fps <= 6, "the helper sends no more frames than maxFps allows")
    }

    /** Swipes up across the middle of the screen in 24 moves, about one a frame. */
    private suspend fun HelperSession.swipe() {
        send(PluginMessage.Touch(TouchPhase.Down, 0.5, 0.75))
        for (step in 1..24) {
            delay(17.milliseconds)
            send(PluginMessage.Touch(TouchPhase.Move, 0.5, 0.75 - 0.45 * step / 24))
        }
        send(PluginMessage.Touch(TouchPhase.Up, 0.5, 0.3))
    }

    private fun openSettings() {
        simctl("terminate", udid, SettingsBundle)
        simctl("launch", udid, SettingsBundle)
        Thread.sleep(1500)
    }

    private fun simctl(vararg arguments: String) {
        ProcessBuilder("xcrun", "simctl", *arguments)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor()
    }
}
