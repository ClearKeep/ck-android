package com.clearkeep.screen.videojanus

interface IControlCall  {
    fun onMuteChange(isOn: Boolean) {}
    fun onFaceTimeChange(isOn: Boolean) {}
    fun onSpeakChange(isOn: Boolean) {}
    fun onCameraChange(isOn: Boolean) {}
}