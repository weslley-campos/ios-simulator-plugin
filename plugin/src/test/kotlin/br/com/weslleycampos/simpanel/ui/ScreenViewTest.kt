package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.keyboard.HidUsage
import br.com.weslleycampos.simpanel.keyboard.Keystroke
import br.com.weslleycampos.simpanel.protocol.DeviceButton
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.protocol.TouchPhase
import br.com.weslleycampos.simpanel.screen.Zoom
import br.com.weslleycampos.simpanel.screen.proMaxFrame
import br.com.weslleycampos.simpanel.simulators.Orientation
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScreenViewTest {
    private fun view(): ScreenView = ScreenView().apply {
        setSize(500, 900)
        attach(Dimension(1206, 2622))
    }

    private fun frame(color: Color) = BufferedImage(400, 870, BufferedImage.TYPE_INT_RGB).apply {
        createGraphics().run {
            this.color = color
            fillRect(0, 0, 400, 870)
            dispose()
        }
    }

    private fun ScreenView.rendered(): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { image ->
            val g = image.createGraphics()
            paint(g)
            g.dispose()
        }

    private fun ScreenView.mouse(id: Int, x: Int, y: Int) {
        val modifiers = if (id == MouseEvent.MOUSE_RELEASED) 0 else InputEvent.BUTTON1_DOWN_MASK
        dispatchEvent(MouseEvent(this, id, 0, modifiers, x, y, 1, false, MouseEvent.BUTTON1))
    }

    @Test
    fun paintsTheFrameFittedAndCenteredInThePanel() {
        val view = view()
        view.show(frame(Color.RED))

        val rendered = view.rendered()

        assertEquals(Color.RED.rgb, rendered.getRGB(250, 450))
        assertEquals(Color.RED.rgb, rendered.getRGB(250, 10))
        assertTrue(rendered.getRGB(20, 450) != Color.RED.rgb, "the sides stay background")
    }

    @Test
    fun roundsTheScreensCornersInsideTheDevice() {
        val view = view().apply { showDeviceFrame(proMaxFrame()) }
        view.show(frame(Color.RED))

        val rendered = view.rendered()

        assertTrue(rendered.getRGB(54, 24) != Color.RED.rgb, "the screen's top left corner stays background")
        assertEquals(Color.RED.rgb, rendered.getRGB(250, 25))
        assertEquals(Color.RED.rgb, rendered.getRGB(250, 450))
    }

    @Test
    fun turnsAClickAndDragIntoATouchThatFollowsThePointer() {
        val view = view()
        val touches = mutableListOf<PluginMessage.Touch>()
        view.onTouch = { touches += it }
        view.show(frame(Color.RED))

        view.mouse(MouseEvent.MOUSE_PRESSED, 250, 450)
        view.mouse(MouseEvent.MOUSE_DRAGGED, 250, 200)
        view.mouse(MouseEvent.MOUSE_RELEASED, 250, -40)

        assertEquals(listOf(TouchPhase.Down, TouchPhase.Move, TouchPhase.Up), touches.map { it.phase })
        assertEquals(0.5, touches[0].x, 0.01)
        assertEquals(0.5, touches[0].y, 0.01)
        assertEquals(0.22, touches[1].y, 0.01)
        assertEquals(0.0, touches[2].y)
    }

    private fun ScreenView.wheel(x: Int, y: Int, rotation: Double, shift: Boolean = false) {
        val modifiers = if (shift) InputEvent.SHIFT_DOWN_MASK else 0
        dispatchEvent(
            MouseWheelEvent(
                this, MouseEvent.MOUSE_WHEEL, 0, modifiers, x, y, x, y, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation.toInt(), rotation,
            ),
        )
    }

    @Test
    fun scrollsWhatLiesUnderThePointerByTheWheelsDistance() {
        val view = view()
        val scrolls = mutableListOf<PluginMessage.Scroll>()
        view.onScroll = { scrolls += it }
        view.show(frame(Color.RED))

        view.wheel(250, 450, rotation = 2.5)
        view.wheel(250, 450, rotation = -1.0, shift = true)

        assertEquals(0.5, scrolls[0].x, 0.01)
        assertEquals(0.5, scrolls[0].y, 0.01)
        assertEquals(0.0, scrolls[0].dx)
        assertEquals(25.0 / 884, scrolls[0].dy, 0.001)
        assertEquals(-10.0 / 407, scrolls[1].dx, 0.001)
        assertEquals(0.0, scrolls[1].dy)
    }

    @Test
    fun scrollsNeitherOutsideTheScreenNorDuringATouch() {
        val view = view()
        val scrolls = mutableListOf<PluginMessage.Scroll>()
        view.onScroll = { scrolls += it }
        view.show(frame(Color.RED))

        view.wheel(20, 450, rotation = 1.0)
        view.mouse(MouseEvent.MOUSE_PRESSED, 250, 450)
        view.wheel(250, 450, rotation = 1.0)

        assertEquals(emptyList(), scrolls)
    }

    @Test
    fun ignoresClicksOutsideTheScreenAndWhileAStatusShows() {
        val view = view()
        val touches = mutableListOf<PluginMessage.Touch>()
        view.onTouch = { touches += it }
        view.show(frame(Color.RED))
        view.mouse(MouseEvent.MOUSE_PRESSED, 20, 450)
        view.mouse(MouseEvent.MOUSE_RELEASED, 20, 450)

        view.showStatus("Attaching to iPhone 18 Pro…")
        view.mouse(MouseEvent.MOUSE_PRESSED, 250, 450)
        view.mouse(MouseEvent.MOUSE_RELEASED, 250, 450)

        assertEquals(emptyList(), touches)
    }

    @Test
    fun showsAProgressBarCenteredUnderABusyStatus() {
        val view = view()

        view.showStatus("Attaching to iPhone 18 Pro…", busy = true)
        view.doLayout()

        val progress = view.components.single() as JProgressBar
        assertTrue(view.busy)
        assertTrue(progress.isVisible && progress.isIndeterminate)
        assertEquals(170, progress.x)
        assertTrue(progress.y in 300..450, "the bar sits under the text, a third of the way down")
    }

    @Test
    fun hidesTheProgressBarOnceTheScreenShows() {
        val view = view()
        view.showStatus("Attaching to iPhone 18 Pro…", busy = true)

        view.show(frame(Color.RED))

        assertFalse(view.busy)
    }

    @Test
    fun showsNoProgressBarUnderAStatusThatWaits() {
        val view = view()
        view.showStatus("Attaching to iPhone 18 Pro…", busy = true)

        view.showStatus("No simulator is booted", actions = listOf(StatusAction("Boot iPhone 18 Pro") {}))

        assertFalse(view.busy)
    }

    @Test
    fun asksForFramesTheSizeOfTheScreen() {
        assertEquals(PluginMessage.Viewport(407, 884), view().viewport)
    }

    @Test
    fun asksForFramesTheSizeOfTheScreenInsideTheDevice() {
        val view = view()
        val viewports = mutableListOf<PluginMessage.Viewport>()
        view.onViewport = { viewports += it }

        view.showDeviceFrame(proMaxFrame())
        assertEquals(PluginMessage.Viewport(392, 852), viewports.last())

        view.frameVisible = false
        assertEquals(PluginMessage.Viewport(407, 884), viewports.last())
    }

    @Test
    fun touchesOnlyTheScreenInsideTheDevice() {
        val view = view().apply { showDeviceFrame(proMaxFrame()) }
        val touches = mutableListOf<PluginMessage.Touch>()
        view.onTouch = { touches += it }
        view.show(frame(Color.RED))

        view.mouse(MouseEvent.MOUSE_PRESSED, 50, 450)
        view.mouse(MouseEvent.MOUSE_RELEASED, 50, 450)
        view.mouse(MouseEvent.MOUSE_PRESSED, 250, 450)

        assertEquals(TouchPhase.Down, touches.single().phase)
        assertEquals(0.5, touches.single().x, 0.01)
        assertEquals(0.5, touches.single().y, 0.01)
    }

    @Test
    fun holdsTheDevicesButtonDownWhileTheMouseHoldsIt() {
        val view = view().apply { showDeviceFrame(proMaxFrame()) }
        val presses = mutableListOf<PluginMessage.Button>()
        view.onButton = { presses += it }
        view.show(frame(Color.RED))

        view.mouse(MouseEvent.MOUSE_PRESSED, 460, 300)
        view.mouse(MouseEvent.MOUSE_RELEASED, 300, 600)

        val power = DeviceButton.Power
        assertEquals(
            listOf(PluginMessage.Button(power, down = true), PluginMessage.Button(power, down = false)),
            presses,
        )
    }

    @Test
    fun typesItsOwnKeysWhileItShowsTheScreen() {
        val view = view()
        val keys = mutableListOf<PluginMessage.Key>()
        view.onKey = { keys += it }
        val typed = { source: Component ->
            KeyEvent(source, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, 'a')
        }

        assertFalse(view.dispatchKey(typed(view)), "no screen shows yet")
        view.show(frame(Color.RED))
        assertFalse(view.dispatchKey(typed(JPanel())), "another component has focus")
        assertTrue(view.dispatchKey(typed(view)))

        assertEquals(Keystroke(HidUsage.letter('a')).presses(), keys)
    }

    /** A screen showing a frame in a scroll pane the size of [view]'s panel. */
    private fun scrolled(): Pair<ScreenView, JScrollPane> {
        val view = ScreenView()
        val scrollPane = JScrollPane(view).apply {
            border = null
            setSize(500, 900)
            // Without a window, a scroll pane lays itself out only once it's displayable.
            addNotify()
            validate()
        }
        view.attach(Dimension(1206, 2622))
        view.show(frame(Color.RED))
        return view to scrollPane
    }

    @Test
    fun showsTheZoomTheScreenIsShownAtOnceItShows() {
        val view = view()
        assertNull(view.zoomLevels)

        view.show(frame(Color.RED))

        val levels = assertNotNull(view.zoomLevels)
        assertEquals(407.0 / 1206, levels.shown, 0.001)
        assertEquals(levels.shown, levels.fit)
    }

    @Test
    fun zoomsTheScreenInItsScrollPaneAroundTheMiddle() {
        val (view, scrollPane) = scrolled()
        val viewports = mutableListOf<PluginMessage.Viewport>()
        view.onViewport = { viewports += it }

        view.zoom = Zoom.ActualSize

        assertEquals(Dimension(1222, 2638), view.size)
        assertEquals(PluginMessage.Viewport(1206, 2622), viewports.last())
        val levels = assertNotNull(view.zoomLevels)
        assertEquals(1.0, levels.shown)
        assertTrue(levels.fit < 0.34, "fitting still fits the scroll pane, not the zoomed screen")
        val shown = scrollPane.viewport.viewRect
        assertEquals(611.0, shown.centerX, 1.0)
        assertEquals(1319.0, shown.centerY, 1.0)
    }

    @Test
    fun fitsThePanelAgainWhenZoomedToFit() {
        val (view, scrollPane) = scrolled()
        view.zoom = Zoom.ActualSize

        view.zoom = Zoom.Fit
        scrollPane.validate()

        assertEquals(Dimension(500, 900), view.size)
        assertEquals(407.0 / 1206, assertNotNull(view.zoomLevels).shown, 0.001)
    }

    @Test
    fun startsFittingAgainWhenAStatusShows() {
        val (view, _) = scrolled()
        view.zoom = Zoom.ActualSize

        view.showStatus("Attaching to iPhone 18 Pro…")

        assertEquals(Zoom.Fit, view.zoom)
        assertNull(view.zoomLevels)
    }

    @Test
    fun scrollsAZoomedScreenWithTheWheelOffTheScreen() {
        val (view, scrollPane) = scrolled()
        val scrolls = mutableListOf<PluginMessage.Scroll>()
        view.onScroll = { scrolls += it }
        view.zoom = Zoom.ActualSize
        scrollPane.viewport.viewPosition = Point(0, 0)

        view.wheel(4, 100, rotation = 3.0)

        assertTrue(scrollPane.viewport.viewPosition.y > 0, "the scroll pane scrolled")
        assertEquals(emptyList(), scrolls)
    }

    /** A screen held with its top to the left, in a panel as wide as [view]'s is tall. */
    private fun turnedLeft(): ScreenView = ScreenView().apply {
        setSize(900, 500)
        attach(Dimension(1206, 2622))
        orientation = Orientation.LandscapeLeft
    }

    /** A frame red at its top and blue at its bottom. */
    private fun splitFrame() = BufferedImage(400, 870, BufferedImage.TYPE_INT_RGB).apply {
        createGraphics().run {
            color = Color.RED
            fillRect(0, 0, 400, 435)
            color = Color.BLUE
            fillRect(0, 435, 400, 435)
            dispose()
        }
    }

    @Test
    fun paintsTheScreenTurnedTheWayTheSimulatorIsHeld() {
        val view = turnedLeft()
        view.show(splitFrame())

        val rendered = view.rendered()

        assertEquals(Color.RED.rgb, rendered.getRGB(150, 250))
        assertEquals(Color.BLUE.rgb, rendered.getRGB(750, 250))
        assertTrue(rendered.getRGB(450, 20) != Color.RED.rgb, "the screen's long side runs across the panel")
    }

    @Test
    fun asksForUprightFramesWhileTheSimulatorIsHeldSideways() {
        val view = turnedLeft()

        assertEquals(PluginMessage.Viewport(407, 884), view.viewport)
    }

    @Test
    fun touchesTheUprightScreenWhereTheTurnedOneIsClicked() {
        val view = turnedLeft()
        val touches = mutableListOf<PluginMessage.Touch>()
        view.onTouch = { touches += it }
        view.show(splitFrame())

        view.mouse(MouseEvent.MOUSE_PRESSED, 100, 250)
        view.mouse(MouseEvent.MOUSE_DRAGGED, 450, 100)
        view.mouse(MouseEvent.MOUSE_RELEASED, 450, 100)

        assertEquals(0.5, touches[0].x, 0.01)
        assertEquals(92.0 / 884, touches[0].y, 0.01)
        assertEquals(0.87, touches[1].x, 0.01)
        assertEquals(0.5, touches[1].y, 0.01)
    }

    @Test
    fun scrollsTheUprightScreenAlongTheWayTheWheelTurnsOnTheTurnedOne() {
        val view = turnedLeft()
        val scrolls = mutableListOf<PluginMessage.Scroll>()
        view.onScroll = { scrolls += it }
        view.show(splitFrame())

        view.wheel(450, 250, rotation = 2.5)

        assertEquals(-25.0 / 407, scrolls[0].dx, 0.001)
        assertEquals(0.0, scrolls[0].dy, 0.001)
    }

    @Test
    fun takesTheTurnedSizeWhenZoomed() {
        val (view, _) = scrolled()
        view.orientation = Orientation.LandscapeLeft

        view.zoom = Zoom.ActualSize

        assertEquals(Dimension(2638, 1222), view.preferredSize)
    }
}
