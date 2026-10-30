package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.protocol.TouchPhase
import br.com.weslleycampos.simpanel.screen.fitScreen
import br.com.weslleycampos.simpanel.screen.fractionOn
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StatusText
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

/** Paints the simulator's latest frame, fitted to the panel, and turns the mouse into touches. */
class ScreenView : JComponent() {
    var onTouch: (PluginMessage.Touch) -> Unit = {}
    var onViewport: (PluginMessage.Viewport) -> Unit = {}

    private var device: Dimension? = null
    private var image: BufferedImage? = null
    private var touching = false
    private val status = object : StatusText(this) {
        override fun isStatusVisible() = image == null
    }

    /** The space frames may fill, in pixels, so the helper sends them at the panel's resolution. */
    val viewport: PluginMessage.Viewport?
        get() {
            val area = area().takeUnless { it.isEmpty } ?: return null
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
                if (image == null || !screen.contains(e.point)) return
                touching = true
                touch(TouchPhase.Down, screen, e.point)
            }

            override fun mouseDragged(e: MouseEvent) {
                if (touching) screen()?.let { touch(TouchPhase.Move, it, e.point) }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (!touching || !SwingUtilities.isLeftMouseButton(e)) return
                touching = false
                screen()?.let { touch(TouchPhase.Up, it, e.point) }
            }
        }
        addMouseListener(mouse)
        addMouseMotionListener(mouse)
    }

    fun attach(device: Dimension) {
        this.device = device
        repaint()
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
                g2.drawImage(frame, screen.x, screen.y, screen.width, screen.height, null)
            } finally {
                g2.dispose()
            }
        }
        status.paint(this, g)
    }

    private fun area(): Rectangle {
        val inset = JBUI.scale(ScreenInset)
        return Rectangle(inset, inset, width - 2 * inset, height - 2 * inset)
    }

    private fun screen(): Rectangle? = device?.let { fitScreen(area(), it) }?.takeUnless { it.isEmpty }

    private fun touch(phase: TouchPhase, screen: Rectangle, point: Point) {
        val fraction = fractionOn(screen, point)
        onTouch(PluginMessage.Touch(phase, fraction.x, fraction.y))
    }
}
