package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.keyboard.DeviceKeyboard
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.protocol.TouchPhase
import br.com.weslleycampos.simpanel.screen.DeviceFrame
import br.com.weslleycampos.simpanel.screen.FrameButton
import br.com.weslleycampos.simpanel.screen.PlacedFrame
import br.com.weslleycampos.simpanel.screen.ScaledImage
import br.com.weslleycampos.simpanel.screen.Zoom
import br.com.weslleycampos.simpanel.screen.ZoomLevels
import br.com.weslleycampos.simpanel.screen.fractionOn
import br.com.weslleycampos.simpanel.screen.placeFrame
import br.com.weslleycampos.simpanel.screen.placeScreen
import br.com.weslleycampos.simpanel.screen.turnOnto
import br.com.weslleycampos.simpanel.screen.upright
import br.com.weslleycampos.simpanel.simulators.Orientation
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
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.JViewport
import javax.swing.Scrollable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val ScreenInset = 8

/** The progress bar a busy status shows, below its text. */
private const val ProgressWidth = 160
private const val ProgressGap = 8

/** The space between the links of a status. */
private const val ActionGap = "      "

private const val NanosPerSecond = 1_000_000_000L
private const val FpsRepaintMillis = 1000

/** Pixels a scroll bar's arrow scrolls a zoomed screen by. */
private const val ScrollUnit = 16

/** Points one wheel unit scrolls; macOS sends a unit for every ten points a trackpad moves. */
private const val WheelUnit = 10.0

/**
 * Paints the simulator's latest frame, fitted to the panel or at a fixed [zoom] in a scroll pane, turned
 * the way the simulator is held and dressed in the device when the helper sent one. It turns the mouse
 * into touches, scrolls and button presses, and the keyboard, while it has focus, into the simulator's.
 * The screen and the device are placed upright, as the frames and the touches see them, and turned
 * onto the component when painted.
 */
class ScreenView : JComponent(), Scrollable {
    var onTouch: (PluginMessage.Touch) -> Unit = {}
    var onScroll: (PluginMessage.Scroll) -> Unit = {}
    var onViewport: (PluginMessage.Viewport) -> Unit = {}
    var onButton: (PluginMessage.Button) -> Unit = {}
    var onKey: (PluginMessage.Key) -> Unit = {}

    /** Pastes the Mac clipboard, for ⌘V. */
    var onPaste: () -> Unit = {}

    /** Types text the simulator's keyboard has no keys for by pasting it. */
    var onPasteText: (String) -> Unit = {}

    /** Called when [zoomLevels] may have changed: with the zoom, the panel's size or the screen itself. */
    var onZoomChange: () -> Unit = {}

    /** Draws the device around the screen, once the helper has sent it. */
    var frameVisible = true
        set(value) {
            field = value
            revalidate()
            layoutChanged()
        }

    /** How large the screen is shown; a new simulator, or a status in its place, starts it fitting the panel. */
    var zoom: Zoom = Zoom.Fit
        set(value) {
            if (field == value) return
            val viewport = parent as? JViewport
            val center = viewport?.let(::centerOf)
            field = value
            revalidate()
            if (viewport != null && center != null) recenter(viewport, center)
            layoutChanged()
        }

    /** Which way up the simulator is held; the screen and the device around it turn with it. */
    var orientation = Orientation.Portrait
        set(value) {
            if (field == value) return
            field = value
            revalidate()
            layoutChanged()
        }

    private var device: Dimension? = null
    private var deviceFrame: DeviceFrame? = null
    private var image: BufferedImage? = null
    private var touching = false
    private var pressed: FrameButton? = null
    private val keyboard = DeviceKeyboard(
        onKey = { onKey(it) },
        onPaste = { onPaste() },
        onPasteText = { onPasteText(it) },
    )
    private val status = object : StatusText(this) {
        override fun isStatusVisible() = image == null
    }
    private val progress = JProgressBar().apply {
        isIndeterminate = true
        isVisible = false
    }

    /** When each frame of the last second or so arrived, for [showFps]. */
    private val frameTimes = ArrayDeque<Long>()
    private val fpsRepaint = Timer(FpsRepaintMillis) { if (image != null) repaint() }

    /** Paints how many frames arrived in the last second over the screen. */
    var showFps = false
        set(value) {
            field = value
            if (value) fpsRepaint.start() else fpsRepaint.stop()
            frameTimes.clear()
            repaint()
        }

    /** The space frames may fill, in pixels, so the helper sends them at the panel's resolution. */
    val viewport: PluginMessage.Viewport?
        get() {
            val area = (screen() ?: upright(area(), orientation)).takeUnless { it.isEmpty } ?: return null
            val scale = pixelScale()
            return PluginMessage.Viewport(
                width = (area.width * scale).roundToInt(),
                height = (area.height * scale).roundToInt(),
            )
        }

    /** The zoom the screen is shown at, and the one fitting it in the panel would pick, while it's shown. */
    val zoomLevels: ZoomLevels?
        get() {
            val device = device?.takeIf { image != null } ?: return null
            val shown = screen() ?: return null
            val fitted = screen(fitArea(), scale = null) ?: return null
            val pixels = pixelScale() / device.width
            return ZoomLevels(shown = shown.width * pixels, fit = fitted.width * pixels)
        }

    init {
        isFocusable = true
        isOpaque = true
        focusTraversalKeysEnabled = false
        background = JBColor.PanelBackground
        add(progress)
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                viewport?.let(onViewport)
                onZoomChange()
            }
        })
        val mouse = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                requestFocusInWindow()
                val screen = screen() ?: return
                if (image == null) return
                val point = upright(e.point)
                if (screen.contains(point)) {
                    touching = true
                    touch(TouchPhase.Down, screen, point)
                    return
                }
                val button = placedFrame()?.buttonAt(point) ?: return
                pressed = button
                onButton(PluginMessage.Button(button.button, down = true))
                repaint()
            }

            override fun mouseDragged(e: MouseEvent) {
                if (touching) screen()?.let { touch(TouchPhase.Move, it, upright(e.point)) }
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
                screen()?.let { touch(TouchPhase.Up, it, upright(e.point)) }
            }

            override fun mouseMoved(e: MouseEvent) {
                val overButton = image != null && placedFrame()?.buttonAt(upright(e.point)) != null
                cursor = Cursor.getPredefinedCursor(if (overButton) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR)
            }
        }
        addMouseListener(mouse)
        addMouseMotionListener(mouse)
        addMouseWheelListener(::scroll)
        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                keyboard.releaseAll()
            }
        })
    }

    /** Types [e] on the simulator while the screen has focus; true when the IDE shouldn't see it. */
    fun dispatchKey(e: KeyEvent): Boolean = e.component === this && image != null && keyboard.dispatch(e)

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
        val first = image == null
        image = frame
        if (showFps) frameTimes.addLast(System.nanoTime())
        if (first) progress.isVisible = false
        repaint()
        if (first) onZoomChange()
    }

    /**
     * Replaces the screen with [text], an optional grey [detail] line and a line of [actions] links,
     * over a progress bar while [busy].
     */
    fun showStatus(
        text: String,
        detail: String? = null,
        actions: List<StatusAction> = emptyList(),
        busy: Boolean = false,
    ) {
        image = null
        touching = false
        pressed = null
        zoom = Zoom.Fit
        keyboard.releaseAll()
        status.clear()
        status.appendText(text)
        detail?.let { status.appendLine(it, SimpleTextAttributes.GRAYED_ATTRIBUTES, null) }
        actions.forEachIndexed { index, action ->
            if (index == 0) {
                status.appendLine(action.text, SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) { action.run() }
            } else {
                status.appendText(ActionGap)
                status.appendText(action.text, SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) { action.run() }
            }
        }
        progress.isVisible = busy
        revalidate()
        repaint()
        onZoomChange()
    }

    /** Whether the status shows a progress bar. */
    val busy: Boolean get() = progress.isVisible

    override fun doLayout() {
        val width = JBUI.scale(ProgressWidth)
        val top = status.pointBelow.y + JBUI.scale(ProgressGap)
        progress.setBounds((this.width - width) / 2, top, width, progress.preferredSize.height)
    }

    override fun getPreferredSize(): Dimension = zoomedSize() ?: super.getPreferredSize()

    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize

    override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) =
        JBUI.scale(ScrollUnit)

    override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) =
        if (orientation == SwingConstants.VERTICAL) visibleRect.height else visibleRect.width

    override fun getScrollableTracksViewportWidth() = zoomedSize()?.let { parent.width >= it.width } ?: true

    override fun getScrollableTracksViewportHeight() = zoomedSize()?.let { parent.height >= it.height } ?: true

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        val frame = image
        val screen = screen()
        if (frame != null && screen != null) {
            val g2 = g.create() as Graphics2D
            try {
                g2.transform(turn())
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
        if (showFps && frame != null) paintFps(g)
    }

    private fun paintFps(g: Graphics) {
        val now = System.nanoTime()
        while (frameTimes.isNotEmpty() && now - frameTimes.first() > NanosPerSecond) frameTimes.removeFirst()
        val shown = visibleRect
        val inset = JBUI.scale(ScreenInset)
        g.font = JBUI.Fonts.smallFont()
        g.color = JBColor.GRAY
        g.drawString("${frameTimes.size} fps", shown.x + inset, shown.y + inset + g.fontMetrics.ascent)
    }

    private fun layoutChanged() {
        viewport?.let(onViewport)
        repaint()
        onZoomChange()
    }

    /** Where the middle of what [viewport] shows falls on this component, as fractions of its size. */
    private fun centerOf(viewport: JViewport): Point2D.Double? {
        if (width <= 0 || height <= 0) return null
        val shown = viewport.viewRect
        return Point2D.Double(shown.centerX / width, shown.centerY / height)
    }

    /** Lays [viewport] out at the new zoom and scrolls it to show [center] in its middle again. */
    private fun recenter(viewport: JViewport, center: Point2D.Double) {
        viewport.parent?.validate()
        val extent = viewport.extentSize
        fun position(fraction: Double, size: Int, shown: Int) =
            (fraction * size - shown / 2.0).roundToInt().coerceIn(0, (size - shown).coerceAtLeast(0))
        viewport.viewPosition = Point(
            position(center.x, width, extent.width),
            position(center.y, height, extent.height),
        )
    }

    private fun area(): Rectangle = inset(size)

    /** The space fitting the screen fills: the scroll pane's, as a zoomed screen may be larger. */
    private fun fitArea(): Rectangle = inset((parent as? JViewport)?.size ?: size)

    private fun inset(size: Dimension): Rectangle {
        val inset = JBUI.scale(ScreenInset)
        return Rectangle(inset, inset, size.width - 2 * inset, size.height - 2 * inset)
    }

    /** The Mac's display pixels per pixel of this component, 2 on a Retina display. */
    private fun pixelScale(): Double = graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0

    /** The component's pixels per pixel of the simulator's screen at a fixed zoom; null fits the panel. */
    private fun screenScale(): Double? = (zoom as? Zoom.Fixed)?.let { it.scale / pixelScale() }

    private fun visibleFrame(): DeviceFrame? = deviceFrame?.takeIf { frameVisible }

    /** The component's pixels per point of [frame] that show the screen at [screenScale]. */
    private fun frameScale(frame: DeviceFrame, device: Dimension, screenScale: Double) =
        screenScale * device.width / frame.screen.width

    /** The device, upright, placed to show in [area] at [scale]. */
    private fun placedFrame(area: Rectangle = area(), scale: Double? = screenScale()): PlacedFrame? {
        val frame = visibleFrame() ?: return null
        val device = device ?: return null
        return placeFrame(upright(area, orientation), frame, scale?.let { frameScale(frame, device, it) })
    }

    /** The screen, upright, placed to show in [area] at [scale]. */
    private fun screen(area: Rectangle = area(), scale: Double? = screenScale()): Rectangle? {
        val upright = upright(area, orientation)
        val placed = placedFrame(area, scale)?.screen ?: device?.let { placeScreen(upright, it, scale) }
        return placed?.takeUnless { it.isEmpty }
    }

    /** Turns the upright screen and device onto the component. */
    private fun turn(): AffineTransform = turnOnto(area(), orientation)

    /** Where [point] on the component falls on the upright screen and device. */
    private fun upright(point: Point): Point2D = turn().inverseTransform(point, null)

    /** The size the screen, its device and the inset take at a fixed zoom, or null when they fit the panel. */
    private fun zoomedSize(): Dimension? {
        val scale = screenScale() ?: return null
        val device = device ?: return null
        val frame = visibleFrame()
        val (uprightWidth, uprightHeight) = when (frame) {
            null -> device.width * scale to device.height * scale
            else -> frameScale(frame, device, scale).let { frame.width * it to frame.height * it }
        }
        val (width, height) = if (orientation.sideways) uprightHeight to uprightWidth else uprightWidth to uprightHeight
        val inset = 2 * JBUI.scale(ScreenInset)
        return Dimension(ceil(width).toInt() + inset, ceil(height).toInt() + inset)
    }

    private fun touch(phase: TouchPhase, screen: Rectangle, point: Point2D) {
        val fraction = fractionOn(screen, point)
        onTouch(PluginMessage.Touch(phase, fraction.x, fraction.y))
    }

    /**
     * Scrolls what lies under the pointer; macOS sends a sideways scroll with Shift held. Off the screen,
     * the wheel scrolls a zoomed screen in its scroll pane instead.
     */
    private fun scroll(e: MouseWheelEvent) {
        if (touching) return
        val upright = upright(e.point)
        val screen = screen()?.takeIf { image != null && it.contains(upright) }
        if (screen == null) {
            val scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this) ?: return
            scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, scrollPane))
            return
        }
        val point = fractionOn(screen, upright)
        val distance = e.preciseWheelRotation * WheelUnit
        val shown = if (e.isShiftDown) Point2D.Double(distance, 0.0) else Point2D.Double(0.0, distance)
        val delta = turn().createInverse().deltaTransform(shown, null)
        onScroll(PluginMessage.Scroll(point.x, point.y, dx = delta.x / screen.width, dy = delta.y / screen.height))
    }

    private fun Graphics2D.drawButtons(placed: PlacedFrame, onTop: Boolean) {
        for ((button, bounds) in placed.buttons) {
            if (button.onTop == onTop) drawScaled(if (button === pressed) button.pressedImage else button.image, bounds)
        }
    }

    /** Draws [image] into [bounds] from a copy at the pixel size it lands at, so it stays smooth. */
    private fun Graphics2D.drawScaled(image: ScaledImage, bounds: Rectangle) {
        val scale = hypot(transform.scaleX, transform.shearY)
        val pixels = image.at((bounds.width * scale).roundToInt(), (bounds.height * scale).roundToInt())
        drawImage(pixels, bounds.x, bounds.y, bounds.width, bounds.height, null)
    }
}
