package br.com.weslleycampos.simpanel.simulators

import java.io.IOException

class SimctlException(command: String, output: String) : IOException("$command failed: ${output.trim()}")
