package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.helper.HelperBinary
import br.com.weslleycampos.simpanel.helper.HelperEvent
import br.com.weslleycampos.simpanel.helper.HelperSession
import br.com.weslleycampos.simpanel.protocol.DeviceButton
import br.com.weslleycampos.simpanel.protocol.ErrorCode
import br.com.weslleycampos.simpanel.protocol.HelperMessage
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.simulators.Simctl
import br.com.weslleycampos.simpanel.simulators.Simulator
import br.com.weslleycampos.simpanel.simulators.screenshotFile
import br.com.weslleycampos.simpanel.ui.actions.ControlAction
import br.com.weslleycampos.simpanel.ui.actions.SimulatorControls
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.nio.file.Path
import java.time.LocalDateTime
import javax.imageio.ImageIO
import javax.swing.JPanel

private const val LastUdidKey = "simpanel.lastUdid"
private const val ToolbarPlace = "SimPanelToolbar"
private const val ControlsPlace = "SimPanelControls"
private val Log = logger<SimulatorPanel>()
private val ScreenshotDirectory = Path.of(System.getProperty("user.home"), "Desktop")

/**
 * The tool window's content: a device picker and the simulator's controls above the live screen
 * of the picked simulator.
 */
class SimulatorPanel(private val project: Project) :
    SimpleToolWindowPanel(true, true), Disposable, SimulatorControls {
    private val service = project.service<SimPanelService>()
    private val scope = service.scope.childScope(
        name = "iOS Simulator panel",
        context = Dispatchers.EDT + ModalityState.any().asContextElement(),
    )
    private val devices = ComboBox<Simulator>()
    private val screen = ScreenView()
    private var session: HelperSession? = null
    private var sessionJob: Job? = null
    private var openSimulator: Simulator? = null

    private var toolbars = emptyList<ActionToolbar>()

    override var attached = false
        private set(value) {
            field = value
            toolbars.forEach { it.updateActionsAsync() }
        }

    init {
        devices.renderer = SimulatorRenderer()
        devices.addActionListener { (devices.selectedItem as? Simulator)?.let(::open) }
        val refresh = object : DumbAwareAction(message("action.refresh"), null, AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                refresh()
            }
        }
        val shutDown = ControlAction(message("action.shutDown"), AllIcons.Actions.Suspend, this) { shutDown() }
        val actions = ActionManager.getInstance()
            .createActionToolbar(ToolbarPlace, DefaultActionGroup(refresh, shutDown), true)
        actions.targetComponent = this
        val controls = ActionManager.getInstance()
            .createActionToolbar(ControlsPlace, controlActions(), true)
        controls.targetComponent = this
        controls.layoutStrategy = ToolbarLayoutStrategy.WRAP_STRATEGY
        toolbars = listOf(actions, controls)
        toolbar = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 4)
            add(JPanel(PickerRowLayout(devices, actions.component)).apply {
                add(devices)
                add(actions.component)
            }, BorderLayout.NORTH)
            add(controls.component, BorderLayout.SOUTH)
        }
        setContent(screen)
        screen.onTouch = { session?.send(it) }
        screen.onViewport = { session?.send(it) }
        screen.showStatus(message("status.loading"))
        refresh()
    }

    override fun dispose() {
        scope.cancel()
    }

    override fun press(vararg buttons: DeviceButton) {
        val session = session ?: return
        for (button in buttons) {
            session.send(PluginMessage.Button(button, down = true))
            session.send(PluginMessage.Button(button, down = false))
        }
    }

    override fun takeScreenshot() {
        val simulator = openSimulator ?: return
        scope.launch {
            try {
                val file = screenshotFile(ScreenshotDirectory, simulator.name, LocalDateTime.now())
                val image = withContext(Dispatchers.IO) {
                    Simctl.screenshot(simulator.udid, file)
                    ImageIO.read(file.toFile())
                }
                CopyPasteManager.getInstance().setContents(ImageTransferable(image))
                SimPanelNotifications.screenshotSaved(project, file)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                SimPanelNotifications.failed(project, message("screenshot.failed"), e.message)
            }
        }
    }

    override fun paste() {
        val simulator = openSimulator ?: return
        val text = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor) ?: return
        scope.launch {
            try {
                withContext(Dispatchers.IO) { Simctl.pbcopy(simulator.udid, text) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                SimPanelNotifications.failed(project, message("paste.failed"), e.message)
            }
        }
    }

    override fun shutDown() {
        val simulator = openSimulator ?: return
        sessionJob?.cancel()
        sessionJob = scope.launch {
            screen.showStatus(message("status.shuttingDown", simulator.name))
            try {
                withContext(Dispatchers.IO) { Simctl.shutdown(simulator.udid) }
                service.opened.remove(simulator.udid)
                screen.showStatus(
                    message("status.shutDown", simulator.name),
                    actionText = message("status.boot"),
                    action = { open(simulator.copy(booted = false)) },
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                val failed = message("status.shutDownFailed", simulator.name)
                screen.showStatus(failed, e.message, action = { open(simulator) })
            }
            refresh(openSelected = false)
        }
    }

    private fun controlActions() = DefaultActionGroup(
        ControlAction(message("action.power"), ControlIcons.Power, this) { press(DeviceButton.Power) },
        ControlAction(message("action.volumeUp"), ControlIcons.VolumeUp, this) { press(DeviceButton.VolumeUp) },
        ControlAction(message("action.volumeDown"), ControlIcons.VolumeDown, this) { press(DeviceButton.VolumeDown) },
        Separator.create(),
        ControlAction(message("action.home"), ControlIcons.Home, this) { press(DeviceButton.Home) },
        ControlAction(message("action.appSwitcher"), ControlIcons.AppSwitcher, this) {
            press(DeviceButton.Home, DeviceButton.Home)
        },
        Separator.create(),
        ControlAction(message("action.screenshot"), ControlIcons.Screenshot, this) { takeScreenshot() },
        ControlAction(message("action.paste"), AllIcons.Actions.MenuPaste, this) { paste() },
    )

    private fun refresh(openSelected: Boolean = true) {
        scope.launch {
            val simulators = try {
                withContext(Dispatchers.IO) { Simctl.list() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                screen.showStatus(message("status.listFailed"), e.message, action = ::refresh)
                return@launch
            }
            val keep = (devices.selectedItem as? Simulator)?.udid
                ?: PropertiesComponent.getInstance().getValue(LastUdidKey)
            val selected = simulators.firstOrNull { it.udid == keep } ?: simulators.firstOrNull { it.booted }
            devices.model = CollectionComboBoxModel(simulators, selected)
            if (!openSelected) return@launch
            when {
                simulators.isEmpty() -> screen.showStatus(message("status.noSimulators"))
                selected == null -> screen.showStatus(message("status.pick"))
                else -> open(selected)
            }
        }
    }

    private fun open(simulator: Simulator) {
        if (simulator.udid == openSimulator?.udid && sessionJob?.isActive == true) return
        sessionJob?.cancel()
        openSimulator = simulator
        service.opened[simulator.udid] = simulator
        PropertiesComponent.getInstance().setValue(LastUdidKey, simulator.udid)
        sessionJob = scope.launch {
            try {
                attach(simulator)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                screen.showStatus(message("status.failed", simulator.name), e.message, action = { open(simulator) })
            }
        }
    }

    private suspend fun attach(simulator: Simulator) {
        if (!simulator.booted) {
            screen.showStatus(message("status.booting", simulator.name))
            withContext(Dispatchers.IO) { Simctl.boot(simulator.udid) }
            refresh()
        }
        screen.showStatus(message("status.attaching", simulator.name))
        val session = HelperSession(HelperBinary.path(), simulator.udid)
        this.session = session
        screen.viewport?.let(session::send)
        var failure: HelperMessage.Failure? = null
        try {
            session.run { event ->
                when (event) {
                    is HelperEvent.Attached -> {
                        attached = true
                        screen.attach(Dimension(event.hello.width, event.hello.height))
                    }
                    is HelperEvent.FrameReady -> screen.show(event.image)
                    is HelperEvent.Failed -> failure = event.failure
                }
            }
        } finally {
            if (this.session === session) {
                this.session = null
                attached = false
            }
        }
        // The picker's list is a snapshot, so a simulator it shows booted may have shut down since.
        if (failure?.errorCode == ErrorCode.DeviceNotBooted && simulator.booted) {
            return attach(simulator.copy(booted = false))
        }
        val retry = { open(simulator) }
        when (val reason = failure) {
            null -> screen.showStatus(message("status.stopped", simulator.name), action = retry)
            else -> screen.showStatus(reason.errorCode.text(), reason.message, action = retry)
        }
    }
}
