package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.helper.HelperBinary
import br.com.weslleycampos.simpanel.helper.HelperEvent
import br.com.weslleycampos.simpanel.helper.HelperSession
import br.com.weslleycampos.simpanel.protocol.HelperMessage
import br.com.weslleycampos.simpanel.simulators.Simctl
import br.com.weslleycampos.simpanel.simulators.Simulator
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
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
import javax.swing.JPanel

private const val LastUdidKey = "simpanel.lastUdid"
private const val ToolbarPlace = "SimPanelToolbar"
private val Log = logger<SimulatorPanel>()

/** The tool window's content: a device picker above the live screen of the picked simulator. */
class SimulatorPanel(project: Project) : SimpleToolWindowPanel(true, true), Disposable {
    private val scope = project.service<SimPanelService>().scope.childScope(
        name = "iOS Simulator panel",
        context = Dispatchers.EDT + ModalityState.any().asContextElement(),
    )
    private val devices = ComboBox<Simulator>()
    private val screen = ScreenView()
    private var session: HelperSession? = null
    private var sessionJob: Job? = null
    private var openUdid: String? = null

    init {
        devices.renderer = SimulatorRenderer()
        devices.addActionListener { (devices.selectedItem as? Simulator)?.let(::open) }
        val refresh = object : DumbAwareAction(message("action.refresh"), null, AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                refresh()
            }
        }
        val actions = ActionManager.getInstance()
            .createActionToolbar(ToolbarPlace, DefaultActionGroup(refresh), true)
        actions.targetComponent = this
        toolbar = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 4)
            add(devices, BorderLayout.WEST)
            add(actions.component, BorderLayout.CENTER)
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

    private fun refresh() {
        scope.launch {
            val simulators = try {
                withContext(Dispatchers.IO) { Simctl.list() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                screen.showStatus(message("status.listFailed"), e.message, retry = ::refresh)
                return@launch
            }
            val keep = (devices.selectedItem as? Simulator)?.udid
                ?: PropertiesComponent.getInstance().getValue(LastUdidKey)
            val selected = simulators.firstOrNull { it.udid == keep } ?: simulators.firstOrNull { it.booted }
            devices.model = CollectionComboBoxModel(simulators, selected)
            when {
                simulators.isEmpty() -> screen.showStatus(message("status.noSimulators"))
                selected == null -> screen.showStatus(message("status.pick"))
                else -> open(selected)
            }
        }
    }

    private fun open(simulator: Simulator) {
        if (simulator.udid == openUdid && sessionJob?.isActive == true) return
        sessionJob?.cancel()
        openUdid = simulator.udid
        PropertiesComponent.getInstance().setValue(LastUdidKey, simulator.udid)
        sessionJob = scope.launch {
            try {
                attach(simulator)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                screen.showStatus(message("status.failed", simulator.name), e.message, retry = { open(simulator) })
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
                    is HelperEvent.Attached -> screen.attach(Dimension(event.hello.width, event.hello.height))
                    is HelperEvent.FrameReady -> screen.show(event.image)
                    is HelperEvent.Failed -> failure = event.failure
                }
            }
        } finally {
            if (this.session === session) this.session = null
        }
        val retry = { open(simulator) }
        when (val reason = failure) {
            null -> screen.showStatus(message("status.stopped", simulator.name), retry = retry)
            else -> screen.showStatus(reason.errorCode.text(), reason.message, retry = retry)
        }
    }
}
