package br.com.weslleycampos.simpanel.ui

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

/** Holds the project's coroutine scope, which the platform cancels when the project closes. */
@Service(Service.Level.PROJECT)
class SimPanelService(val scope: CoroutineScope)
