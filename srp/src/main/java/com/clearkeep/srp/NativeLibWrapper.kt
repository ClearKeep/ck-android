package com.clearkeep.srp

class NativeLibWrapper {
    private val nativeLib = NativeLib()

    fun getSalt(username: String, rawPassword: String): ByteArray {
        return nativeLib.getSalt(username, rawPassword)
    }

    fun getVerificator(): ByteArray {
        return nativeLib.getVerificator()
    }

    fun getA(username: String, rawPassword: String): ByteArray {
        return nativeLib.getA(username, rawPassword)
    }

    fun getM(salt: ByteArray, b: ByteArray): ByteArray {
        return nativeLib.getM(salt, b)
    }

    fun freeMemoryCreateAccount() {
        return nativeLib.freeMemoryCreateAccount()
    }

    fun freeMemoryAuthenticate() {
        return nativeLib.freeMemoryAuthenticate()
    }
}