package com.clearkeep.dragonsrp

class NativeLib {
    @JvmField
    var srpClientPtr = 0L
    @JvmField
    var srpClientAuthenticatorPtr = 0L

    @JvmField
    var srpServerPtr = 0L
    @JvmField
    var srpVerificatorPtr = 0L

    external fun stringFromJNI(): String
    external fun getSalt(): String
    external fun getVerificator(username: String, rawPassword: String, salt: String): String
    external fun getA(username: String, rawPassword: String): String
    external fun getM1(username: String, rawPassword: String, newSalt: String, b: String): String
    external fun testVerifyGetSalt(username: String, verificator: String, salt: String, a: String): String
    external fun testVerifyGetB(username: String, verificator: String, salt: String, a: String): String
    external fun testVerifyGetM2(username: String, verificator: String, salt: String, a: String, m1: String): String
    external fun testVerifyGetK(username: String, verificator: String, salt: String, a: String, m1: String): String
    external fun testCreateUserNative(): String
    external fun testClientFlowNative(): String

    companion object {
        // Used to load the 'dragonsrp' library on application startup.
        init {
            System.loadLibrary("dragonsrp")
        }
    }
}