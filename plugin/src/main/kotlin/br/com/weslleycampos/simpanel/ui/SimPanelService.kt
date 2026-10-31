package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.simulators.Simulator
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Holds the project's coroutine scope, which the platform cancels when the project closes, the
 * simulators the panel opened, which closing the project offers to shut down, and the simulators
 * runs start on, which the panel follows.
 */
@Service(Service.Level.PROJECT)
class SimPanelService(val scope: CoroutineScope) {
    /** By UDID, read and written on the EDT. */
    val opened = LinkedHashMap<String, Simulator>()

    /** The UDID of each simulator a run starts on; the last is replayed to a panel created after it. */
    val runs = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
