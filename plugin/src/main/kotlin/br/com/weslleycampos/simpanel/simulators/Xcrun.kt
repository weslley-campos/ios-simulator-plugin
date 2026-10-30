package br.com.weslleycampos.simpanel.simulators

import kotlin.time.Duration

/** Runs the Xcode tool `xcrun` finds, as [execute] does. */
internal fun xcrun(timeout: Duration, vararg arguments: String, input: String? = null): String =
    execute(timeout, "xcrun", *arguments, input = input)
