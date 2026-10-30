package br.com.weslleycampos.simpanel.helper

import br.com.weslleycampos.simpanel.protocol.HelperMessage
import br.com.weslleycampos.simpanel.protocol.MessageReader
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val ConnectTimeout = 5.seconds
private val ConnectRetry = 50.milliseconds
private val ExitGrace = 1.seconds
private val SocketCount = AtomicInteger()
private val Log = logger<HelperSession>()

/** One helper process attached to one simulator, from its start until it disconnects. */
class HelperSession(private val binary: Path, private val udid: String) {
    private val outgoing = Channel<PluginMessage>(Channel.UNLIMITED)

    /** Queues [message]; anything sent before the helper accepts the connection goes out once it does. */
    fun send(message: PluginMessage) {
        outgoing.trySend(message)
    }

    /**
     * Starts the helper and hands [onEvent] what it sends, in the caller's context, until the helper
     * closes the connection. Cancelling closes the connection, which stops the helper.
     */
    suspend fun run(onEvent: suspend (HelperEvent) -> Unit) {
        val socket = Path.of(
            System.getProperty("java.io.tmpdir"),
            "simpanel-${ProcessHandle.current().pid()}-${SocketCount.incrementAndGet()}.sock",
        )
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder(binary.toString(), "--udid", udid, "--socket", socket.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
        thread(isDaemon = true, name = "simpanel-helper $udid") {
            process.errorReader().useLines { lines -> lines.forEach(Log::info) }
        }
        try {
            connect(socket, process).use { channel -> exchange(channel, onEvent) }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                if (!process.waitFor(ExitGrace.inWholeMilliseconds, TimeUnit.MILLISECONDS)) process.destroy()
                Files.deleteIfExists(socket)
            }
        }
    }

    private suspend fun exchange(channel: SocketChannel, onEvent: suspend (HelperEvent) -> Unit) {
        coroutineScope {
            val writer = launch(Dispatchers.IO) {
                for (message in outgoing) runInterruptible { channel.writeFully(message.encoded()) }
            }
            JpegDecoder().use { decoder ->
                val reader = MessageReader(channel)
                while (true) {
                    val event = runInterruptible(Dispatchers.IO) { reader.nextEvent(decoder) } ?: break
                    onEvent(event)
                }
            }
            writer.cancel()
        }
    }

    private suspend fun connect(socket: Path, process: Process): SocketChannel {
        val deadline = TimeSource.Monotonic.markNow() + ConnectTimeout
        while (true) {
            try {
                return withContext(Dispatchers.IO) { SocketChannel.open(UnixDomainSocketAddress.of(socket)) }
            } catch (e: IOException) {
                if (!process.isAlive) throw HelperExitedException(process.exitValue())
                if (deadline.hasPassedNow()) throw e
                delay(ConnectRetry)
            }
        }
    }
}

/** The next message this version handles, skipping the rest, or null once the helper disconnects. */
private fun MessageReader.nextEvent(decoder: JpegDecoder): HelperEvent? {
    while (true) {
        val raw = next() ?: return null
        when (val message = HelperMessage.decode(raw)) {
            is HelperMessage.Hello -> return HelperEvent.Attached(message)
            is HelperMessage.Frame -> return HelperEvent.FrameReady(decoder.decode(message.jpeg))
            is HelperMessage.Failure -> return HelperEvent.Failed(message)
            null -> Unit
        }
    }
}

private fun SocketChannel.writeFully(bytes: ByteArray) {
    val buffer = ByteBuffer.wrap(bytes)
    while (buffer.hasRemaining()) write(buffer)
}
