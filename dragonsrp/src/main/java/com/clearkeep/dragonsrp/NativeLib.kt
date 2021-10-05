package com.clearkeep.dragonsrp

class NativeLib {

    external fun stringFromJNI(): String
    external fun getSalt(): String
    external fun getVerificator(username: String, rawPassword: String, salt: String): String
    external fun getA(username: String, rawPassword: String): String

    companion object {
        // Used to load the 'dragonsrp' library on application startup.
        init {
            System.loadLibrary("dragonsrp")
        }
    }
}