package br.com.weslleycampos.simpanel.simulators

import java.io.IOException

class CommandException(command: String, output: String) : IOException("$command failed: ${output.trim()}")
