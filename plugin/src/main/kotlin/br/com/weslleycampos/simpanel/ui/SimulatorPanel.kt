package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.helper.HelperBinary
import br.com.weslleycampos.simpanel.helper.HelperEvent
import br.com.weslleycampos.simpanel.helper.HelperSession
import br.com.weslleycampos.simpanel.helper.NextStep
import br.com.weslleycampos.simpanel.helper.RestartPolicy
import br.com.weslleycampos.simpanel.helper.SessionEnd
import br.com.weslleycampos.simpanel.keyboard.Keystroke
import br.com.weslleycampos.simpanel.protocol.DeviceButton
import br.com.weslleycampos.simpanel.protocol.HelperMessage
import br.com.weslleycampos.simpanel.protocol.PluginMessage
import br.com.weslleycampos.simpanel.simulators.BootWatch
import br.com.weslleycampos.simpanel.simulators.DeviceHub
import br.com.weslleycampos.simpanel.simulators.Devicectl
import br.com.weslleycampos.simpanel.simulators.Orientation
import br.com.weslleycampos.simpanel.simulators.Rotation
import br.com.weslleycampos.simpanel.simulators.Simctl
import br.com.weslleycampos.simpanel.simulators.Simulator
import br.com.weslleycampos.simpanel.simulators.pickSimulator
import br.com.weslleycampos.simpanel.simulators.screenshotFile
import br.com.weslleycampos.simpanel.ui.actions.ControlAction
import br.com.weslleycampos.simpanel.ui.actions.FrameToggleAction
import br.com.weslleycampos.simpanel.ui.actions.SimulatorControls
import com.intellij.icons.AllIcons
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.application.ApplicationManager
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyEvent
import java.nio.file.Path
import java.time.LocalDateTime
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val LastUdidKey = "simpanel.lastUdid"
private const val FrameVisibleKey = "simpanel.frameVisible"
private const val ToolbarPlace = "SimPanelToolbar"
private const val ControlsPlace = "SimPanelControls"
private val Log = logger<SimulatorPanel>()
private val ScreenshotDirectory = Path.of(System.getProperty("user.home"), "Desktop")

/** How long the simulator takes to paste, so that keys typed after ⌘V land after the text. */
private val PasteSettle = 400.milliseconds

/** How often the panel lists the simulators while it waits for one to boot. */
private val BootPoll = 2.seconds

/**
 * The tool window's content: a device picker and the simulator's controls above the live screen
 * of the picked simulator, with its zoom controls floating over it.
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

    /** Whether the panel shows the next simulator to boot, offering to boot [waitingFor]. */
    private var waitingForBoot = false
    private var waitingFor: Simulator? = null
    private val openDeviceHub = StatusAction(message("status.openDeviceHub")) { openDeviceHub() }

    /** What was typed, in order, so that a paste's text lands before the keys typed after it. */
    private val typing = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    private var toolbars = emptyList<ActionToolbar>()

    override var attached = false
        private set(value) {
            field = value
            toolbars.forEach { it.updateActionsAsync() }
        }

    /** The screen, which takes the keyboard when the tool window is shown. */
    val focusTarget: JComponent get() = screen

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
        val zoomControls = ZoomControls(screen, shortcutsTarget = this)
        setContent(ScreenPane(screen, zoomControls))
        screen.onZoomChange = zoomControls::update
        screen.onTouch = { session?.send(it) }
        screen.onScroll = { session?.send(it) }
        screen.onViewport = { session?.send(it) }
        screen.onButton = { session?.send(it) }
        screen.onKey = { key -> typing.trySend { session?.send(key) } }
        screen.onPaste = ::paste
        screen.onPasteText = ::typeByPasting
        IdeEventQueue.getInstance().addDispatcher(
            IdeEventQueue.EventDispatcher { it is KeyEvent && screen.dispatchKey(it) },
            this,
        )
        scope.launch {
            for (step in typing) step()
        }
        scope.launch { watchForBoot() }
        screen.frameVisible = PropertiesComponent.getInstance().getBoolean(FrameVisibleKey, true)
        screen.showFps = ApplicationManager.getApplication().isInternal
        screen.showStatus(message("status.loading"), busy = true)
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

    override fun rotate(rotation: Rotation) {
        val simulator = openSimulator ?: return
        scope.launch {
            try {
                val orientation = withContext(Dispatchers.IO) { Devicectl.rotate(simulator.udid, rotation) }
                if (openSimulator?.udid == simulator.udid) screen.orientation = orientation
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                SimPanelNotifications.failed(project, message("rotate.failed"), e.message)
            }
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
        CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)?.let(::typeByPasting)
    }

    override var frameVisible: Boolean
        get() = screen.frameVisible
        set(value) {
            screen.frameVisible = value
            PropertiesComponent.getInstance().setValue(FrameVisibleKey, value, true)
        }

    override fun shutDown() {
        val simulator = openSimulator ?: return
        sessionJob?.cancel()
        sessionJob = scope.launch {
            screen.showStatus(message("status.shuttingDown", simulator.name), busy = true)
            try {
                withContext(Dispatchers.IO) { Simctl.shutdown(simulator.udid) }
                service.opened.remove(simulator.udid)
                awaitBoot(simulator.copy(booted = false), message("status.shutDown", simulator.name))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                showFailed(message("status.shutDownFailed", simulator.name), e.message)
            }
        }
    }

    /** Puts [text] on the simulator's pasteboard and presses ⌘V, after what was typed before. */
    private fun typeByPasting(text: String) {
        val simulator = openSimulator ?: return
        typing.trySend {
            try {
                withContext(Dispatchers.IO) { Simctl.pbcopy(simulator.udid, text) }
                Keystroke.Paste.presses().forEach { session?.send(it) }
                delay(PasteSettle)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                SimPanelNotifications.failed(project, message("paste.failed"), e.message)
            }
        }
    }

    private fun controlActions() = DefaultActionGroup(
        ControlAction(message("action.power"), ControlIcons.Power, this) { press(DeviceButton.Power) },
        ControlAction(message("action.volumeUp"), ControlIcons.VolumeUp, this) { press(DeviceButton.VolumeUp) },
        ControlAction(message("action.volumeDown"), ControlIcons.VolumeDown, this) { press(DeviceButton.VolumeDown) },
        ControlAction(message("action.rotateLeft"), ControlIcons.RotateLeft, this) { rotate(Rotation.Left) },
        ControlAction(message("action.rotateRight"), ControlIcons.RotateRight, this) { rotate(Rotation.Right) },
        Separator.create(),
        ControlAction(message("action.home"), ControlIcons.Home, this) { press(DeviceButton.Home) },
        ControlAction(message("action.appSwitcher"), ControlIcons.AppSwitcher, this) {
            press(DeviceButton.Home, DeviceButton.Home)
        },
        Separator.create(),
        ControlAction(message("action.screenshot"), ControlIcons.Screenshot, this) { takeScreenshot() },
        ControlAction(message("action.paste"), AllIcons.Actions.MenuPaste, this) { paste() },
        Separator.create(),
        FrameToggleAction(this),
    )

    /** Lists the simulators and shows one, unless the panel is already busy showing one. */
    private fun refresh() {
        scope.launch {
            val simulators = try {
                withContext(Dispatchers.IO) { Simctl.list() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                showFailed(message("status.listFailed"), e.message)
                return@launch
            }
            when {
                sessionJob?.isActive == true -> list(simulators, simulators.find { it.udid == openSimulator?.udid })
                waitingForBoot -> keepWaiting(simulators, bootedBefore = null)
                else -> show(simulators)
            }
        }
    }

    /** Attaches to a simulator as soon as one boots, while the panel waits for one and is shown. */
    private suspend fun watchForBoot() {
        var bootedBefore: Set<String>? = null
        while (true) {
            delay(BootPoll)
            if (!waitingForBoot || !isShowing) {
                bootedBefore = null
                continue
            }
            val simulators = try {
                withContext(Dispatchers.IO) { Simctl.list() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.debug(e)
                continue
            }
            if (waitingForBoot) keepWaiting(simulators, bootedBefore)
            bootedBefore = simulators.filter { it.booted }.mapTo(HashSet()) { it.udid }
        }
    }

    /** Lists [simulators] and shows the one [pickSimulator] picks, or waits for it to boot. */
    private fun show(simulators: List<Simulator>) {
        val keep = (devices.selectedItem as? Simulator)?.udid
            ?: PropertiesComponent.getInstance().getValue(LastUdidKey)
        val pick = pickSimulator(simulators, keep)
        list(simulators, pick)
        if (pick?.booted == true) open(pick, boot = false) else awaitBoot(pick)
    }

    /** Lists [simulators] and takes the step [BootWatch] gives while the panel waits for one to boot. */
    private fun keepWaiting(simulators: List<Simulator>, bootedBefore: Set<String>?) {
        when (val step = BootWatch.after(simulators, waitingFor, bootedBefore)) {
            is BootWatch.Show -> {
                list(simulators, step.simulator)
                open(step.simulator, boot = false)
            }
            is BootWatch.WaitFor -> {
                list(simulators, step.simulator)
                awaitBoot(step.simulator)
            }
            BootWatch.KeepWaiting -> list(simulators, simulators.find { it.udid == waitingFor?.udid })
        }
    }

    /** Puts [simulators] in the picker with [selected] selected, without opening it. */
    private fun list(simulators: List<Simulator>, selected: Simulator?) {
        val listed = List(devices.itemCount, devices::getItemAt)
        if (simulators != listed || selected != devices.selectedItem) {
            devices.model = CollectionComboBoxModel(simulators, selected)
        }
    }

    /**
     * Opens [simulator], booting it first when [boot] allows, and keeps showing it. A simulator the
     * user picks is booted; one the panel picks is already.
     */
    private fun open(simulator: Simulator, boot: Boolean = true) {
        if (simulator.udid == openSimulator?.udid && sessionJob?.isActive == true) return
        sessionJob?.cancel()
        waitingForBoot = false
        openSimulator = simulator
        service.opened[simulator.udid] = simulator
        PropertiesComponent.getInstance().setValue(LastUdidKey, simulator.udid)
        sessionJob = scope.launch {
            try {
                if (boot) {
                    val text = if (simulator.booted) "status.attaching" else "status.booting"
                    screen.showStatus(message(text, simulator.name), busy = true)
                    withContext(Dispatchers.IO) { Simctl.boot(simulator.udid) }
                    if (!simulator.booted) refresh()
                }
                keepShowing(simulator)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                showFailed(message("status.failed", simulator.name), e.message)
            }
        }
    }

    /** Shows [simulator] until a session ends for good, starting a new helper each time one may help. */
    private suspend fun keepShowing(simulator: Simulator) {
        val restarts = RestartPolicy()
        screen.showStatus(message("status.attaching", simulator.name), busy = true)
        while (true) {
            val (end, shown) = attach(simulator)
            when (val next = restarts.next(end, shown)) {
                is NextStep.Restart -> {
                    Log.info("Restarting the helper for ${simulator.name} in ${next.after} after $end")
                    screen.showStatus(message("status.attaching", simulator.name), busy = true)
                    delay(next.after)
                }
                NextStep.AwaitBoot ->
                    return awaitBoot(simulator.copy(booted = false), message("status.shutDown", simulator.name))
                NextStep.GiveUp -> return when (end) {
                    is SessionEnd.Failed -> showFailed(end.failure.errorCode.text(), end.failure.message)
                    is SessionEnd.Lost -> showFailed(message("status.stopped", simulator.name), end.cause?.message)
                }
            }
        }
    }

    /** Runs one helper session for [simulator], returning how it ended and how long it showed the screen. */
    private suspend fun attach(simulator: Simulator): Pair<SessionEnd, Duration> {
        screen.orientation = Orientation.Portrait
        val session = HelperSession(HelperBinary.path(), simulator.udid)
        this.session = session
        screen.viewport?.let(session::send)
        var failure: HelperMessage.Failure? = null
        var attachedAt: TimeMark? = null
        val end = try {
            coroutineScope {
                launch { showOrientation(simulator) }
                session.run { event ->
                    when (event) {
                        is HelperEvent.Attached -> {
                            attachedAt = TimeSource.Monotonic.markNow()
                            attached = true
                            screen.attach(Dimension(event.hello.width, event.hello.height))
                        }
                        is HelperEvent.FrameReady -> screen.show(event.image)
                        is HelperEvent.DeviceFrameReady -> screen.showDeviceFrame(event.frame)
                        is HelperEvent.Failed -> failure = event.failure
                    }
                }
            }
            failure?.let(SessionEnd::Failed) ?: SessionEnd.Lost(null)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.warn(e)
            SessionEnd.Lost(e)
        } finally {
            if (this.session === session) {
                this.session = null
                attached = false
            }
        }
        return end to (attachedAt?.elapsedNow() ?: Duration.ZERO)
    }

    /**
     * Shows [text] until a simulator boots, with a link that boots [simulator], or opens DeviceHub
     * when there's none to boot.
     */
    private fun awaitBoot(simulator: Simulator?, text: String = message("status.notBooted")) {
        waitingForBoot = true
        waitingFor = simulator
        val action = simulator?.let { StatusAction(message("status.boot", it.name)) { open(it) } }
        screen.showStatus(
            if (simulator == null) message("status.noSimulators") else text,
            actions = listOf(action ?: openDeviceHub),
        )
    }

    /** Shows why the panel can't show a simulator, with Retry and a way to fix it in DeviceHub. */
    private fun showFailed(text: String, detail: String?) {
        waitingForBoot = false
        val retry = StatusAction(message("status.retry"), ::refresh)
        screen.showStatus(text, detail, actions = listOf(retry, openDeviceHub))
    }

    private fun openDeviceHub() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { DeviceHub.open() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.warn(e)
                SimPanelNotifications.failed(project, message("deviceHub.failed"), e.message)
            }
        }
    }

    /** Turns the screen the way the simulator is held, which it keeps from before the panel showed it. */
    private suspend fun showOrientation(simulator: Simulator) {
        try {
            screen.orientation = withContext(Dispatchers.IO) { Devicectl.orientation(simulator.udid) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.warn(e)
        }
    }
}
