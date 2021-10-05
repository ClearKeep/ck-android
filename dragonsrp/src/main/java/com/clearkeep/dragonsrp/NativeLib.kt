package com.clearkeep.dragonsrp

class NativeLib {

    /**
     * A native method that is implemented by the 'dragonsrp' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'dragonsrp' library on application startup.
        init {
            System.loadLibrary("dragonsrp")
        }
    }
}