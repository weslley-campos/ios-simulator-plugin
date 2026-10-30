package br.com.weslleycampos.simpanel.helper

import java.io.IOException
import kotlin.time.Duration

class HelperTimeoutException(timeout: Duration) : IOException("The helper didn't attach within $timeout")
