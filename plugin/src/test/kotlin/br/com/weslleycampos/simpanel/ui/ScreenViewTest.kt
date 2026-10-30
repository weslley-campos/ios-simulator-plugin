package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.protocol.DeviceButton
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.protocol.TouchPhase
import br.com.weslleycampos.simpanel.screen.proMaxFrame
import java.awt.Color
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun asksForFramesAtThePanelsSizeLessItsInset() {
        assertEquals(PluginMessage.Viewport(484, 884), view().viewport)
    }

    @Test
    fun asksForFramesTheSizeOfTheScreenInsideTheDevice() {
        val view = view()
        val viewports = mutableListOf<PluginMessage.Viewport>()
        view.onViewport = { viewports += it }

        view.showDeviceFrame(proMaxFrame())
        assertEquals(PluginMessage.Viewport(392, 852), viewports.last())

        view.frameVisible = false
        assertEquals(PluginMessage.Viewport(484, 884), viewports.last())
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
}
