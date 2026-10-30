package br.com.weslleycampos.simpanel

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BundleName = "messages.SimPanelBundle"

object SimPanelBundle : DynamicBundle(BundleName) {
    @Nls
    fun message(@PropertyKey(resourceBundle = BundleName) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
