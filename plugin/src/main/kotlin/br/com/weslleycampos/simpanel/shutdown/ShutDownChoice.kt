package br.com.weslleycampos.simpanel.shutdown

import br.com.weslleycampos.simpanel.SimPanelBundle.message

/** What closing a project does with the simulators its panel opened, set in Advanced Settings. */
enum class ShutDownChoice {
    Ask,
    ShutDown,
    KeepRunning;

    /** The label Advanced Settings shows. */
    override fun toString() = message("shutDownChoice.$name")
}
