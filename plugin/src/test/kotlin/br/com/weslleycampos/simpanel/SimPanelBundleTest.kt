package br.com.weslleycampos.simpanel

import java.text.MessageFormat
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

class SimPanelBundleTest {
    private val messages = Properties().apply {
        SimPanelBundleTest::class.java.getResourceAsStream("/messages/SimPanelBundle.properties")!!.use(::load)
    }

    /** The platform formats a message only when it has parameters, and MessageFormat drops a lone quote. */
    @Test
    fun keepsTheApostrophesOfMessagesWithParameters() {
        val parameterized = messages.stringPropertyNames().filter { messages.getProperty(it).contains('{') }

        for (key in parameterized) {
            val value = messages.getProperty(key)
            val formatted = MessageFormat.format(value, "X")

            assertEquals(value.replace("''", "'").replace("{0}", "X"), formatted, key)
        }
    }
}
