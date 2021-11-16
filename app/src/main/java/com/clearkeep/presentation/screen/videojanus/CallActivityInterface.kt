package com.clearkeep.presentation.screen.videojanus

interface CallActivityInterface {
    fun switchAudioToVideoMode()
    fun setMic(isMuted: Boolean)
    fun setSpeaker(isMuted: Boolean)
    fun setVideoStatus(isMuted: Boolean)

    /**
     * When call:
     * - onCreated with audio mode
     * - state from calling to answer
     */
    fun updateUIAudioMode()

    /**
     * When call:
     * - switch from audio to video
     * - state from calling to answer
     * - a new member joined
     */
    fun updateUIVideoMode()
}