package com.clearkeep.presentation.screen.videojanus

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.usecase.call.CancelCallUseCase
import com.clearkeep.domain.usecase.call.RequestVideoCallUseCase
import com.clearkeep.domain.usecase.call.SwitchAudioToVideoCallUseCase
import com.clearkeep.januswrapper.*
import com.clearkeep.presentation.screen.videojanus.common.CallState
import com.clearkeep.presentation.screen.videojanus.common.createVideoCapture
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.ServerResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.json.JSONObject
import org.webrtc.*
import video_call.VideoCallOuterClass
import java.lang.ref.WeakReference
import java.math.BigInteger
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val requestVideoCallUseCase: RequestVideoCallUseCase,
    private val switchAudioToVideoCallUseCase: SwitchAudioToVideoCallUseCase,
    private val cancelCallUseCase: CancelCallUseCase
) : ViewModel(), JanusRTCInterface,
    PeerConnectionClient.PeerConnectionEvents, IControlCall {

    val rootEglBase by lazy { EglBase.create() }
    private val peerConnectionClient by lazy { PeerConnectionClient() }
    private lateinit var mContext: WeakReference<Context>
    private var mWebSocketChannel: WebSocketChannel? = null
    private lateinit var mLocalSurfaceRenderer: WeakReference<SurfaceViewRenderer>
    var mCurrentCallState = MutableLiveData(CallState.CALLING)
    var listenerOnRemoteRenderAdd: ((JanusConnection) -> Unit)? = null
    var listenerOnPublisherJoined: (() -> Unit)? = null
    var mIsAudioMode = MutableLiveData<Boolean>(null)
    var totalTimeRun: Long = 0
    var totalTimeRunJob: Job? = null
    var mIsMute = MutableLiveData(false)

    fun startVideo(
        context: Context, mLocalSurfaceRenderer: SurfaceViewRenderer,
        ourClientId: String,
        janusGroupId: Int, janusUrl: String,
        stunUrl: String, turnUrl: String,
        turnUser: String, turnPass: String,
        token: String
    ) {
        printlnCK(
            "startVideo: stun = $stunUrl, turn = $turnUrl, username = $turnUser, pwd = $turnPass" +
                    ", group = $janusGroupId, token = $token Janus URL: $janusUrl"
        )
        this.mContext = WeakReference(context)
        this.mLocalSurfaceRenderer = WeakReference(mLocalSurfaceRenderer)
        mWebSocketChannel = WebSocketChannel(janusGroupId, ourClientId, token, janusUrl)
        mWebSocketChannel!!.initConnection()
        mWebSocketChannel!!.setDelegate(this)
        val peerConnectionParameters = PeerConnectionClient.PeerConnectionParameters(
            false, 360, 480, 20, "VP8",
            true, 0, "opus", false,
            false, false, false, false,
            turnUrl, turnUser, turnPass, stunUrl
        )
        peerConnectionClient.createPeerConnectionFactory(
            mContext.get(),
            peerConnectionParameters,
            this
        )
        peerConnectionClient.startVideoSource()
    }

    suspend fun requestVideoCall(groupId: Int, isAudioMode: Boolean, owner: Owner): ServerResponse? {
        return requestVideoCallUseCase(groupId, isAudioMode, owner)
    }

    suspend fun switchAudioToVideoCall(groupId: Int, owner: Owner): Boolean {
        return switchAudioToVideoCallUseCase(groupId, owner)
    }

    suspend fun cancelCall(groupId: Int, owner: Owner): Boolean {
        return cancelCallUseCase(groupId, owner)
    }

    override fun onPublisherJoined(handleId: BigInteger) {
        offerPeerConnection(handleId)
        Log.e("antx", "onPublisherJoined")
    }

    override fun onPublisherRemoteJsep(handleId: BigInteger, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient.setRemoteDescription(handleId, sessionDescription)
    }

    override fun subscriberHandleRemoteJsep(janusHandle: JanusHandle, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient.subscriberHandleRemoteJsep(
            janusHandle.handleId,
            janusHandle.display,
            sessionDescription
        )
    }

    override fun onLeaving(handleId: BigInteger) {
        printlnCK("onLeaving: $handleId")
        removeRemoteRender(handleId)
    }

    override fun onLocalDescription(sdp: SessionDescription, handleId: BigInteger) {
        printlnCK(sdp.type.toString())
        mWebSocketChannel?.publisherCreateOffer(handleId, sdp)
    }

    override fun onRemoteDescription(sdp: SessionDescription, handleId: BigInteger) {
        printlnCK(sdp.type.toString())
        mWebSocketChannel?.subscriberCreateAnswer(handleId, sdp)
    }

    override fun onIceCandidate(candidate: IceCandidate?, handleId: BigInteger) {
        if (candidate != null) {
            mWebSocketChannel?.trickleCandidate(handleId, candidate)
        } else {
            mWebSocketChannel?.trickleCandidateComplete(handleId)
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}

    override fun onIceConnected() {
    }

    override fun onIceDisconnected() {
    }

    override fun onPeerConnectionClosed() {
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {}

    override fun onPeerConnectionError(description: String) {
        printlnCK("onPeerConnectionError: $description")
    }

    override fun onRemoteRenderAdded(connection: JanusConnection) {
        listenerOnRemoteRenderAdd?.invoke(connection)
        printlnCK("onRemoteRenderAdded: ${connection.handleId}")
        if (mCurrentCallState.value != CallState.ANSWERED) {
            mCurrentCallState.postValue(CallState.ANSWERED)
        }
    }

    override fun onRemoteRenderRemoved(connection: JanusConnection) {
        removeRemoteRender(connection.handleId)
    }

    private fun removeRemoteRender(handleId: BigInteger) {
        mCurrentCallState.postValue(CallState.ENDED)
    }

    private fun offerPeerConnection(handleId: BigInteger) {
        peerConnectionClient.createPeerConnection(
            rootEglBase.eglBaseContext,
            mLocalSurfaceRenderer.get(),
            mContext.get()?.let { createVideoCapture(it) },
            handleId
        )
        peerConnectionClient.createOffer(handleId)
        listenerOnPublisherJoined?.invoke()
    }

    override fun onCameraChange(isOn: Boolean) {
        peerConnectionClient.switchCamera()
    }

    override fun onFaceTimeChange(isOn: Boolean) {
        peerConnectionClient.setLocalVideoEnable(isOn)
    }

    override fun onMuteChange(isOn: Boolean) {
        peerConnectionClient.setAudioEnabled(!isOn)
    }

    override fun onSpeakChange(isOn: Boolean) {
        setSpeakerphoneOn(isOn)
    }

    private fun setSpeakerphoneOn(isOn: Boolean) {
        try {
            val audioManager: AudioManager =
                mContext.get()?.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = isOn
        } catch (e: Exception) {
            printlnCK("setSpeakerphoneOn, $e")
        }
    }

    override fun onCleared() {
        super.onCleared()
        mWebSocketChannel?.close()
        peerConnectionClient.close()
        totalTimeRunJob?.cancel()
    }
}