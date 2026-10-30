package br.com.weslleycampos.simpanel.protocol

import java.io.IOException

class PayloadTooLargeException(val size: Long) :
    IOException("A $size-byte payload is over the ${Framing.MaxPayloadSize}-byte limit")
