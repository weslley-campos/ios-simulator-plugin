package br.com.weslleycampos.simpanel.helper

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val ListenTimeout = 5.seconds

/** The real helper, before any simulator is involved, which it needs only once the plugin connects. */
class HelperProcessTest {
    private val helper = Path.of(System.getProperty("simpanel.helper"))

    @Test
    fun exitsWhenTheProcessThatStartedItDies() {
        val socket = Path.of(System.getProperty("java.io.tmpdir"), "simpanel-${ProcessHandle.current().pid()}-p.sock")
        val parent = ProcessBuilder("/bin/sh", "-c", "\"$0\" --udid none --socket \"$1\" & wait", "$helper", "$socket")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        var child: ProcessHandle? = null
        try {
            val listenBy = TimeSource.Monotonic.markNow() + ListenTimeout
            while (!Files.exists(socket)) {
                check(listenBy.hasNotPassedNow()) { "the helper didn't listen within $ListenTimeout" }
                Thread.sleep(20)
            }
            child = parent.toHandle().children().findFirst().orElseThrow()

            parent.destroyForcibly()

            assertTrue(runCatching { child.onExit().get(2, TimeUnit.SECONDS) }.isSuccess, "the helper exited")
            assertFalse(Files.exists(socket), "the helper removed its socket")
        } finally {
            child?.destroyForcibly()
            parent.destroyForcibly()
            Files.deleteIfExists(socket)
        }
    }
}
