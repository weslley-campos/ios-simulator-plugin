package br.com.weslleycampos.simpanel.ui

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.lang.reflect.Method
import javax.swing.SwingUtilities

/**
 * Runs each test on the EDT, as the IDE runs Swing code. Off it, `revalidate()` only queues the
 * invalidation on the EDT, so a layout right after it may or may not see the change.
 */
class OnEdt : InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        var failure: Throwable? = null
        SwingUtilities.invokeAndWait {
            try {
                invocation.proceed()
            } catch (e: Throwable) {
                failure = e
            }
        }
        failure?.let { throw it }
    }
}
