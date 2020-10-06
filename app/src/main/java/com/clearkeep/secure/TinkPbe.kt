package com.clearkeep.secure

import java.io.IOException
import java.security.GeneralSecurityException

object TinkPbe {

    @Throws(GeneralSecurityException::class, IOException::class)
    fun encrypt(plaintextString: String, secretKey: ByteArray?): ByteArray? {

        return null
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun decrypt(ciphertextByte: ByteArray, secretKey: ByteArray?): String? {
        return null
    }

}