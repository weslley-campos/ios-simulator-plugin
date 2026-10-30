package br.com.weslleycampos.simpanel.helper

import java.io.IOException

class HelperExitedException(val status: Int) :
    IOException("The helper exited with status $status before it accepted the connection")
