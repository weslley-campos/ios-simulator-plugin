package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.ui.actions.ZoomAction
import br.com.weslleycampos.simpanel.ui.actions.ZoomLevelAction
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

private const val ZoomPlace = "SimPanelZoom"
private const val ExpandedKey = "simpanel.zoomControlsExpanded"

/**
 * The zoom buttons floating over the screen in a pill, as on Running Devices: in, out, to actual
 * size and to fit, above the zoom the screen is shown at, which the pill folds down to. Their ⌘
 * shortcuts work while the keyboard is in [shortcutsTarget].
 */
class ZoomControls(private val screen: ScreenView, shortcutsTarget: JComponent) : JPanel(BorderLayout()) {
    private var expanded = PropertiesComponent.getInstance().getBoolean(ExpandedKey, true)
        set(value) {
            field = value
            PropertiesComponent.getInstance().setValue(ExpandedKey, value, true)
            toolbar.updateActionsAsync()
        }

    private val toolbar: ActionToolbar

    init {
        val zoomIn = ZoomAction(message("zoom.in"), AllIcons.General.Add, screen) { zoomedIn }
        val zoomOut = ZoomAction(message("zoom.out"), AllIcons.General.Remove, screen) { zoomedOut }
        val actualSize = ZoomAction(message("zoom.actual"), AllIcons.General.ActualZoom, screen) { actualSize }
        val fit = ZoomAction(message("zoom.fit"), AllIcons.General.FitContent, screen) { fitted }
        zoomIn.registerCustomShortcutSet(command(KeyEvent.VK_EQUALS, KeyEvent.VK_ADD), shortcutsTarget)
        zoomOut.registerCustomShortcutSet(command(KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT), shortcutsTarget)
        actualSize.registerCustomShortcutSet(command(KeyEvent.VK_PERIOD, KeyEvent.VK_NUMPAD0), shortcutsTarget)
        fit.registerCustomShortcutSet(command(KeyEvent.VK_SLASH, KeyEvent.VK_DIVIDE), shortcutsTarget)
        val level = ZoomLevelAction(screen, expanded = { expanded }, toggle = { expanded = !expanded })
        val fold = object : DumbAwareAction(message("zoom.hide"), null, AllIcons.General.ChevronDown) {
            override fun actionPerformed(e: AnActionEvent) {
                expanded = false
            }
        }
        val unfolded = arrayOf(zoomIn, zoomOut, actualSize, fit, level, fold)
        val folded = arrayOf<AnAction>(level)
        val group = object : ActionGroup() {
            override fun getChildren(e: AnActionEvent?) = if (expanded) unfolded else folded
        }
        toolbar = ActionManager.getInstance().createActionToolbar(ZoomPlace, group, false)
        toolbar.targetComponent = shortcutsTarget
        toolbar.layoutStrategy = CenteredColumnLayout
        toolbar.component.isOpaque = false
        toolbar.component.border = JBUI.Borders.empty()
        isOpaque = false
        background = JBColor.PanelBackground
        border = JBUI.Borders.empty(4, 2)
        add(toolbar.component)
        isVisible = false
    }

    /** Shows the zoom the screen is shown at now, and the pill only while it shows the simulator. */
    fun update() {
        isVisible = screen.zoomLevels != null
        toolbar.updateActionsAsync()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val arc = width.toDouble()
            val pill = RoundRectangle2D.Double(0.5, 0.5, width - 1.0, height - 1.0, arc, arc)
            g2.color = background
            g2.fill(pill)
            g2.color = JBColor.border()
            g2.draw(pill)
        } finally {
            g2.dispose()
        }
    }
}

private fun command(vararg keyCodes: Int) = CustomShortcutSet(
    *keyCodes.map { KeyboardShortcut(KeyStroke.getKeyStroke(it, InputEvent.META_DOWN_MASK), null) }.toTypedArray(),
)
