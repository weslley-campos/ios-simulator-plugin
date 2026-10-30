package br.com.weslleycampos.simpanel.simulators

import java.io.IOException

class XcrunException(command: String, output: String) : IOException("$command failed: ${output.trim()}")
