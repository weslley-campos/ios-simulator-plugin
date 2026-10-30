package br.com.weslleycampos.simpanel.helper

import br.com.weslleycampos.simpanel.protocol.HelperMessage
import br.com.weslleycampos.simpanel.protocol.MessageReader
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.screen.DeviceFrame
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** How long the helper has from its start to say hello, or why it can't. */
private val AttachTimeout = 5.seconds
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
     * closes the connection. Cancelling closes the connection, which stops the helper. Throws
     * [HelperTimeoutException] when the helper sends nothing within [AttachTimeout].
     */
    suspend fun run(onEvent: suspend (HelperEvent) -> Unit) {
        val attachBy = TimeSource.Monotonic.markNow() + AttachTimeout
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
            connect(socket, process, attachBy).use { channel -> exchange(channel, attachBy, onEvent) }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                if (!process.waitFor(ExitGrace.inWholeMilliseconds, TimeUnit.MILLISECONDS)) process.destroy()
                Files.deleteIfExists(socket)
            }
        }
    }

    private suspend fun exchange(
        channel: SocketChannel,
        attachBy: TimeMark,
        onEvent: suspend (HelperEvent) -> Unit,
    ) {
        coroutineScope {
            val writer = launch(Dispatchers.IO) {
                for (message in outgoing) runInterruptible { channel.writeFully(message.encoded()) }
            }
            JpegDecoder().use { decoder ->
                val reader = MessageReader(channel)
                suspend fun next() = runInterruptible(Dispatchers.IO) { reader.nextEvent(decoder) }
                var event = try {
                    withTimeout(-attachBy.elapsedNow()) { next() }
                } catch (_: TimeoutCancellationException) {
                    throw HelperTimeoutException(AttachTimeout)
                }
                while (event != null) {
                    onEvent(event)
                    event = next()
                }
            }
            writer.cancel()
        }
    }

    private suspend fun connect(socket: Path, process: Process, deadline: TimeMark): SocketChannel {
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
            is HelperMessage.Chrome -> try {
                return HelperEvent.DeviceFrameReady(DeviceFrame.of(message))
            } catch (e: IOException) {
                Log.warn("Showing the screen without the device frame", e)
            }
            null -> Unit
        }
    }
}

private fun SocketChannel.writeFully(bytes: ByteArray) {
    val buffer = ByteBuffer.wrap(bytes)
    while (buffer.hasRemaining()) write(buffer)
}
