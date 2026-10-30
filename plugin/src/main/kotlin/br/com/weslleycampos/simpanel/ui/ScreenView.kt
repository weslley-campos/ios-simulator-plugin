package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.protocol.TouchPhase
import br.com.weslleycampos.simpanel.screen.DeviceFrame
import br.com.weslleycampos.simpanel.screen.FrameButton
import br.com.weslleycampos.simpanel.screen.PlacedFrame
import br.com.weslleycampos.simpanel.screen.ScaledImage
import br.com.weslleycampos.simpanel.screen.fitScreen
import br.com.weslleycampos.simpanel.screen.fractionOn
import br.com.weslleycampos.simpanel.screen.placeFrame
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StatusText
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

private const val ScreenInset = 8

/**
 * Paints the simulator's latest frame, fitted to the panel and dressed in the device when the helper
 * sent one, and turns the mouse into touches and button presses.
 */
class ScreenView : JComponent() {
    var onTouch: (PluginMessage.Touch) -> Unit = {}
    var onViewport: (PluginMessage.Viewport) -> Unit = {}
    var onButton: (PluginMessage.Button) -> Unit = {}

    /** Draws the device around the screen, once the helper has sent it. */
    var frameVisible = true
        set(value) {
            field = value
            layoutChanged()
        }

    private var device: Dimension? = null
    private var deviceFrame: DeviceFrame? = null
    private var image: BufferedImage? = null
    private var touching = false
    private var pressed: FrameButton? = null
    private val status = object : StatusText(this) {
        override fun isStatusVisible() = image == null
    }

    /** The space frames may fill, in pixels, so the helper sends them at the panel's resolution. */
    val viewport: PluginMessage.Viewport?
        get() {
            val area = (placedFrame()?.screen ?: area()).takeUnless { it.isEmpty } ?: return null
            val scale = graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0
            return PluginMessage.Viewport(
                width = (area.width * scale).roundToInt(),
                height = (area.height * scale).roundToInt(),
            )
        }

    init {
        isFocusable = true
        background = JBColor.PanelBackground
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                viewport?.let(onViewport)
            }
        })
        val mouse = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                requestFocusInWindow()
                val screen = screen() ?: return
                if (image == null) return
                if (screen.contains(e.point)) {
                    touching = true
                    touch(TouchPhase.Down, screen, e.point)
                    return
                }
                val button = placedFrame()?.buttonAt(e.point) ?: return
                pressed = button
                onButton(PluginMessage.Button(button.button, down = true))
                repaint()
            }

            override fun mouseDragged(e: MouseEvent) {
                if (touching) screen()?.let { touch(TouchPhase.Move, it, e.point) }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                pressed?.let { button ->
                    pressed = null
                    onButton(PluginMessage.Button(button.button, down = false))
                    repaint()
                }
                if (!touching) return
                touching = false
                screen()?.let { touch(TouchPhase.Up, it, e.point) }
            }

            override fun mouseMoved(e: MouseEvent) {
                val overButton = image != null && placedFrame()?.buttonAt(e.point) != null
                cursor = Cursor.getPredefinedCursor(if (overButton) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR)
            }
        }
        addMouseListener(mouse)
        addMouseMotionListener(mouse)
    }

    /** Starts a new session's screen, bare until [showDeviceFrame] dresses it. */
    fun attach(device: Dimension) {
        this.device = device
        deviceFrame = null
        layoutChanged()
    }

    fun showDeviceFrame(frame: DeviceFrame) {
        deviceFrame = frame
        layoutChanged()
    }

    fun show(frame: BufferedImage) {
        image = frame
        repaint()
    }

    /** Replaces the screen with [text], an optional grey [detail] line and an optional [action] link. */
    fun showStatus(
        text: String,
        detail: String? = null,
        actionText: String = message("status.retry"),
        action: (() -> Unit)? = null,
    ) {
        image = null
        touching = false
        pressed = null
        status.clear()
        status.appendText(text)
        detail?.let { status.appendLine(it, SimpleTextAttributes.GRAYED_ATTRIBUTES, null) }
        action?.let { run ->
            status.appendLine(actionText, SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) { run() }
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        val frame = image
        val screen = screen()
        if (frame != null && screen != null) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                val placed = placedFrame()
                placed?.let { g2.drawButtons(it, onTop = false) }
                val clip = g2.clip
                placed?.let { g2.clip(it.screenClip) }
                g2.drawImage(frame, screen.x, screen.y, screen.width, screen.height, null)
                g2.clip = clip
                placed?.let {
                    g2.drawScaled(it.frame.bezel, it.device)
                    g2.drawButtons(it, onTop = true)
                }
            } finally {
                g2.dispose()
            }
        }
        status.paint(this, g)
    }

    private fun layoutChanged() {
        viewport?.let(onViewport)
        repaint()
    }

    private fun area(): Rectangle {
        val inset = JBUI.scale(ScreenInset)
        return Rectangle(inset, inset, width - 2 * inset, height - 2 * inset)
    }

    private fun placedFrame(): PlacedFrame? =
        deviceFrame?.takeIf { frameVisible && device != null }?.let { placeFrame(area(), it) }

    private fun screen(): Rectangle? =
        (placedFrame()?.screen ?: device?.let { fitScreen(area(), it) })?.takeUnless { it.isEmpty }

    private fun touch(phase: TouchPhase, screen: Rectangle, point: Point) {
        val fraction = fractionOn(screen, point)
        onTouch(PluginMessage.Touch(phase, fraction.x, fraction.y))
    }

    private fun Graphics2D.drawButtons(placed: PlacedFrame, onTop: Boolean) {
        for ((button, bounds) in placed.buttons) {
            if (button.onTop == onTop) drawScaled(if (button === pressed) button.pressedImage else button.image, bounds)
        }
    }

    /** Draws [image] into [bounds] from a copy at the pixel size it lands at, so it stays smooth. */
    private fun Graphics2D.drawScaled(image: ScaledImage, bounds: Rectangle) {
        val scale = transform.scaleX
        val pixels = image.at((bounds.width * scale).roundToInt(), (bounds.height * scale).roundToInt())
        drawImage(pixels, bounds.x, bounds.y, bounds.width, bounds.height, null)
    }
}
