package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.simulators.Simulator
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

/**
 * Holds the project's coroutine scope, which the platform cancels when the project closes, and the
 * simulators the panel opened, which closing the project offers to shut down.
 */
@Service(Service.Level.PROJECT)
class SimPanelService(val scope: CoroutineScope) {
    /** By UDID, read and written on the EDT. */
    val opened = LinkedHashMap<String, Simulator>()
}
