package com.clearkeep.features.calls.presentation

interface IControlCall {
    fun onMuteChange(isOn: Boolean) {}
    fun onFaceTimeChange(isOn: Boolean) {}
    fun onSpeakChange(isOn: Boolean) {}
    fun onCameraChange(isOn: Boolean) {}
}