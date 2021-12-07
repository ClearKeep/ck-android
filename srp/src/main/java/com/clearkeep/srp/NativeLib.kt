package com.clearkeep.srp

internal class NativeLib {
    @JvmField
    var verificatorPtr = 0L

    @JvmField
    var usrPtr = 0L

    external fun getSalt(username: String, rawPassword: String): ByteArray
    external fun getVerificator(): ByteArray
    external fun getA(username: String, rawPassword: String): ByteArray
    external fun getM(salt: ByteArray, b: ByteArray): ByteArray
    external fun freeMemoryCreateAccount()
    external fun freeMemoryAuthenticate()

    companion object {
        // Used to load the 'srp' library on application startup.
        init {
            System.loadLibrary("srp")
        }
    }
}