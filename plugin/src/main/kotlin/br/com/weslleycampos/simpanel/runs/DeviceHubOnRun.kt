package br.com.weslleycampos.simpanel.runs

import br.com.weslleycampos.simpanel.SimPanelBundle.message

/** What the panel does with the DeviceHub window a run opens on its simulator, set in Advanced Settings. */
enum class DeviceHubOnRun {
    Hide,
    LeaveOpen;

    /** The label Advanced Settings shows. */
    override fun toString() = message("deviceHubOnRun.$name")
}
