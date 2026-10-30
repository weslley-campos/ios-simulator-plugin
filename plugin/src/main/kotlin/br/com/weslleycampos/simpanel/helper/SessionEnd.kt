package br.com.weslleycampos.simpanel.helper

import br.com.weslleycampos.simpanel.protocol.HelperMessage

/** Why a helper session ended without the panel stopping it. */
sealed interface SessionEnd {
    /** The helper said why. */
    data class Failed(val failure: HelperMessage.Failure) : SessionEnd

    /** The helper exited, disconnected or didn't attach in time without saying why. */
    data class Lost(val cause: Throwable?) : SessionEnd
}
