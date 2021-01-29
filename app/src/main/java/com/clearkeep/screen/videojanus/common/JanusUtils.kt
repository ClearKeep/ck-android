package com.clearkeep.screen.videojanus.common

import android.content.Context
import com.clearkeep.utilities.printlnCK
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.VideoCapturer

private fun useCamera2(context: Context): Boolean {
    return Camera2Enumerator.isSupported(context)
}

private fun captureToTexture(): Boolean {
    return true
}

private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
    val deviceNames = enumerator.deviceNames

    // First, try to find front facing camera
    printlnCK("Looking for front facing cameras.")
    for (deviceName in deviceNames) {
        if (enumerator.isFrontFacing(deviceName)) {
            printlnCK("Creating front facing camera capture.")
            val videoCapture: VideoCapturer? = enumerator.createCapturer(deviceName, null)
            if (videoCapture != null) {
                return videoCapture
            }
        }
    }

    // Front facing camera not found, try something else
    printlnCK("Looking for other cameras.")
    for (deviceName in deviceNames) {
        if (!enumerator.isFrontFacing(deviceName)) {
            printlnCK("Creating other camera capture.")
            val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
            if (videoCapturer != null) {
                return videoCapturer
            }
        }
    }
    return null
}

fun createVideoCapture(context: Context): VideoCapturer? {
    var videoCapture: VideoCapturer? = null
    videoCapture = if (useCamera2(context)) {
        printlnCK("Creating capture using camera2 API.")
        createCameraCapturer(Camera2Enumerator(context))
    } else {
        printlnCK("Creating capture using camera1 API.")
        createCameraCapturer(Camera1Enumerator(captureToTexture()))
    }
    if (videoCapture == null) {
        printlnCK("Failed to open camera")
        return null
    }
    return videoCapture
}