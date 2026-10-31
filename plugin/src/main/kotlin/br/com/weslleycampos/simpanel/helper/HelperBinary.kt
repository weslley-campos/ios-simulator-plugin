package br.com.weslleycampos.simpanel.helper

import com.intellij.ide.plugins.cl.PluginAwareClassLoader
import java.nio.file.Files
import java.nio.file.Path

object HelperBinary {
    /** The helper in the plugin's bin directory, made executable again if unpacking dropped the bit. */
    fun path(): Path {
        // The plugin's own class loader names it the same way in every IDE version.
        val plugin = checkNotNull((HelperBinary::class.java.classLoader as? PluginAwareClassLoader)?.pluginDescriptor) {
            "The plugin's classes weren't loaded by the IDE"
        }
        return executable(plugin.pluginPath.resolve("bin/simpanel-helper"))
    }

    /** [binary], made executable if it isn't, as installing the plugin from a zip drops the bit. */
    internal fun executable(binary: Path): Path {
        if (!Files.isExecutable(binary)) binary.toFile().setExecutable(true)
        return binary
    }
}
