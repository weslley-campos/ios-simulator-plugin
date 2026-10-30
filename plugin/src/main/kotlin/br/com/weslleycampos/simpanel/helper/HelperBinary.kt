package br.com.weslleycampos.simpanel.helper

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.nio.file.Files
import java.nio.file.Path

private const val SimPanelPluginId = "br.com.weslleycampos.simpanel"

object HelperBinary {
    /** The helper in the plugin's bin directory, made executable again if unpacking dropped the bit. */
    fun path(): Path {
        val plugin = checkNotNull(PluginManagerCore.getPlugin(PluginId.getId(SimPanelPluginId))) {
            "The $SimPanelPluginId plugin isn't loaded"
        }
        return executable(plugin.pluginPath.resolve("bin/simpanel-helper"))
    }

    /** [binary], made executable if it isn't, as installing the plugin from a zip drops the bit. */
    internal fun executable(binary: Path): Path {
        if (!Files.isExecutable(binary)) binary.toFile().setExecutable(true)
        return binary
    }
}
