package com.unknown.homecctv

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule

class CCTVService : Service(), JavaAudioDeviceModule.SamplesReadyCallback {

    companion object {
        const val TAG = "myLog.CCTVService"
        var isRunning = false
    }

    private val mContext by lazy { applicationContext }
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val mHandler by lazy { Handler(mainLooper) }
    private val mDatabaseRoom by lazy {
        FirebaseDatabase.getInstance().getReference("rooms").child(C.cctvId)
    }

    private val videoTrackID = "VideoTrack1"
    private val audioTrackID = "AudioTrack1"
    private val localMediaStreamLabel = "MediaStream1"

    private var isCaller = false

    private var isPeerConnected = false

    private var isRemoteSet = false

    private lateinit var sdpConstraints: MediaConstraints

    private lateinit var remoteAudioTrack: AudioTrack
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var mediaStream: MediaStream

    var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null
    private var videoCapturer: VideoCapturer? = null
    private var dataChannel: DataChannel? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        isRunning = true

        val filter = IntentFilter()
        filter.addAction(C.ACTION_FCM)
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, filter)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy")

        isRunning = false

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val msgReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val what = intent?.getStringExtra("what") ?: ""
            val msg = intent?.getStringExtra("msg") ?: ""

            Log.e(TAG, "msgReceiver : what:$what msg:$msg")
            Log.e(TAG, "STATUS : <<<<<<<<<< ${C.status} >>>>>>>>>>")

            when (what) {
                C.CONNECT_CCTV -> {
                    createPeerConnection()
                }
                C.DISCONNECT_CCTV -> {
                    disconnectAll()
                }
            }
        }
    }

    private fun createPeerConnection() {
        Log.e(TAG, "createPeerConnection")

        try {
            if (rootEglBase == null) {
                rootEglBase = EglBase.create()
            }

            val defaultVideoEncoderFactory =
                DefaultVideoEncoderFactory(rootEglBase?.eglBaseContext, true, true)
            val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)

            val initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(mContext)
                    .setFieldTrials(null)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)

            val peerConnectionOption = PeerConnectionFactory.Options()
            peerConnectionOption.disableNetworkMonitor = true

            val adm = createJavaAudioDevice() as AudioDeviceModule

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(peerConnectionOption)
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory()
            adm.release()

            val iceServers = ArrayList<PeerConnection.IceServer>()
            val iceBuilder = PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302")
            iceBuilder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
//                .setUsername("")
//                .setPassword("")
            val iceServer = iceBuilder.createIceServer()
            iceServers.add(iceServer)

//            val iceServerList = listOf("stun.l.google.com:19302")
//            for (server in iceServerList) {
//                val iceStunServerBuilder = PeerConnection.IceServer.builder(server)
//                    .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
//
//                iceServers.add(iceStunServerBuilder.createIceServer())
//            }

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
//            rtcConfig.enableCpuOveruseDetection = false

            sdpConstraints = MediaConstraints()
            sdpConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveAudio",
                    "true"
                )
            )
            sdpConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    "false"
                )
            )

            peerConnection = peerConnectionFactory?.createPeerConnection(
                rtcConfig,
                peerConnectionObserver
            ) as PeerConnection

            dataChannel =
                peerConnection?.createDataChannel("dataChannel", DataChannel.Init())

            getLocalMedia()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun getLocalMedia() {
        Log.e(TAG, "getLocalMedia")

        try {

            if (rootEglBase == null) {
                rootEglBase = EglBase.create()
            }

            val surfaceTextureHelper =
                SurfaceTextureHelper.create("VideoCaptureThread", rootEglBase?.eglBaseContext)
            videoCapturer = createVideoCapturer(this) as VideoCapturer
            val videoSource =
                peerConnectionFactory?.createVideoSource(videoCapturer?.isScreencast ?: false)
            videoCapturer?.initialize(
                surfaceTextureHelper,
                mContext,
                videoSource?.capturerObserver
            )
            localVideoTrack = peerConnectionFactory!!.createVideoTrack(videoTrackID, videoSource)

            videoCapturer?.startCapture(1920, 1080, 30)

            val audioSource = peerConnectionFactory?.createAudioSource(sdpConstraints)
            val localAudioTrack =
                peerConnectionFactory?.createAudioTrack(audioTrackID, audioSource)

            localAudioTrack?.setEnabled(true)
            localAudioTrack?.setVolume(1.0)
            localVideoTrack.setEnabled(true)

            mediaStream = peerConnectionFactory!!.createLocalMediaStream(localMediaStreamLabel)
            mediaStream.addTrack(localAudioTrack)
            mediaStream.addTrack(localVideoTrack)
            peerConnection?.addStream(mediaStream)

            joinRoom()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun joinRoom() {
        Log.e(TAG, "joinRoom..${C.cctvId}")

        mDatabaseRoom.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled:$error")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (s in snapshot.children) {

                    val key = s.key.toString()
                    val data = s.value.toString()

                    if (isCaller && data == "joined") {
                        createOffer()

                    } else if (!isCaller && key == "offer" && !isRemoteSet) {
                        peerConnection?.setRemoteDescription(object : MySdpObserver() {
                            override fun onSetSuccess() {
                                Log.e(TAG, "setRemoteDescription..onSetSuccess")
                                isRemoteSet = true
                                createAnswer()

                            }
                        }, SessionDescription(SessionDescription.Type.OFFER, data))

                    } else if (isCaller && key == "answer" && !isRemoteSet) {
                        peerConnection?.setRemoteDescription(object : MySdpObserver() {
                            override fun onSetSuccess() {
                                Log.e(TAG, "setRemoteDescription..onSetSuccess")
                                isRemoteSet = true
                            }
                        }, SessionDescription(SessionDescription.Type.ANSWER, data))

                    } else if (isCaller && key == "toCaller") {
                        val message = JSONObject(data)
                        val candidate = IceCandidate(
                            message.getString("id"),
                            message.getInt("label"),
                            message.getString("candidate")
                        )
                        peerConnection?.addIceCandidate(candidate)

                    } else if (!isCaller && key == "toCallee") {
                        val message = JSONObject(data)
                        val candidate = IceCandidate(
                            message.getString("id"),
                            message.getInt("label"),
                            message.getString("candidate")
                        )
                        peerConnection?.addIceCandidate(candidate)

                    }
                }
            }

        })

        if (isCaller) {
            FCM.send("${C.CONNECT_CCTV}${C.DIVIDER}${C.cctvId}")
        } else {
            mDatabaseRoom.child("cctv").setValue("joined")
        }

    }

    private fun createVideoCapturer(context: Context): VideoCapturer? {
        val enumerator: CameraEnumerator
        val is2Supported = Camera2Enumerator.isSupported(context)
        Log.e(TAG, "CameraEnumerator is2Supported:$is2Supported")
        enumerator = if (is2Supported) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }

        val deviceNames = enumerator.deviceNames

        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.e(TAG, "!enumerator.isFrontFacing")
                return enumerator.createCapturer(deviceName, null)
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.e(TAG, "enumerator.isFrontFacing")
                return enumerator.createCapturer(deviceName, null)
            }
        }

        return null
    }

    private fun createOffer() {
        Log.e(TAG, "createOffer")
        Log.e(TAG, "peerConnection null?:${peerConnection == null}")
        peerConnection?.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                Log.e(tag, "createOffer .. onCreateSuccess")
                Log.e(tag, "setLocalDescription")

                peerConnection?.setLocalDescription(
                    object : MySdpObserver() {
                        override fun onSetSuccess() {
                            Log.e(tag, "onSetSuccess..")

                            mDatabaseRoom.child("offer")
                                .setValue(sessionDescription?.description)

                        }
                    },
                    sessionDescription
                )
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(tag, "createOffer .. onCreateFailure:$p0")
            }
        }, sdpConstraints)
    }

    fun createAnswer() {
        Log.e(TAG, "createAnswer")
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                Log.e(tag, "createAnswer .. onCreateSuccess")
                Log.e(tag, "setLocalDescription")

                peerConnection?.setLocalDescription(
                    object : MySdpObserver() {
                        override fun onSetSuccess() {
                            Log.e(tag, "onSetSuccess..")

                            mDatabaseRoom.child("answer")
                                .setValue(sessionDescription?.description)
                        }
                    },
                    sessionDescription
                )
            }
        }, sdpConstraints)
    }

    private fun createJavaAudioDevice(): AudioDeviceModule? {
        return JavaAudioDeviceModule.builder(mContext)
            .setSamplesReadyCallback(this)
            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                override fun onWebRtcAudioRecordInitError(p0: String?) {
                    Log.e(TAG, "onWebRtcAudioRecordInitError:$p0")
                }

                override fun onWebRtcAudioRecordError(p0: String?) {
                    Log.e(TAG, "onWebRtcAudioRecordError:$p0")
                }

                override fun onWebRtcAudioRecordStartError(
                    p0: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                    p1: String?
                ) {
                    Log.e(TAG, "onWebRtcAudioRecordStartError:$p1")
                }
            })
            .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                override fun onWebRtcAudioTrackError(p0: String?) {
                    Log.e(TAG, "onWebRtcAudioTrackError:$p0")
                }

                override fun onWebRtcAudioTrackStartError(
                    p0: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                    p1: String?
                ) {
                    Log.e(TAG, "onWebRtcAudioTrackStartError")
                }

                override fun onWebRtcAudioTrackInitError(p0: String?) {
                    Log.e(TAG, "onWebRtcAudioTrackInitError:$p0")
                }
            })
            .createAudioDeviceModule()
    }


    override fun onWebRtcAudioRecordSamplesReady(samples: JavaAudioDeviceModule.AudioSamples?) {
//        samples?.let {
//            if (it.data.isNotEmpty()) {
//                encoder?.onLocalAudioSamplesReady(it.data)
//            }
//        }

    }

    open class MySdpObserver : SdpObserver {
        val tag = "MySdpObserver"
        override fun onSetFailure(p0: String?) {
            Log.e(tag, "MySdpObserver..onSetFailure")
        }

        override fun onSetSuccess() {
            Log.e(tag, "MySdpObserver..onSetSuccess")
        }

        override fun onCreateSuccess(sessionDescription: SessionDescription?) {
            Log.e(tag, "MySdpObserver..onCreateSuccess")
        }

        override fun onCreateFailure(p0: String?) {
            Log.e(tag, "MySdpObserver..onCreateFailure")
        }
    }

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            val message = JSONObject()

            message.put("label", iceCandidate?.sdpMLineIndex)
            message.put("id", iceCandidate?.sdpMid)
            message.put("candidate", iceCandidate?.sdp)

            val mDatabaseTo = if (isCaller) {
                mDatabaseRoom.child("toCallee")
            } else {
                mDatabaseRoom.child("toCaller")
            }

            mDatabaseTo.setValue(message.toString())

        }

        override fun onAddStream(mediaStream: MediaStream) {
            Log.e(
                TAG, "peerConnectionObserver..onAddStream videoTracks size : " +
                        "${mediaStream.videoTracks?.size} .. audioTracks size : ${mediaStream.audioTracks?.size}"
            )
            if (mediaStream.audioTracks.size > 0) {
                remoteAudioTrack = mediaStream.audioTracks.first()
                remoteAudioTrack.setEnabled(true)
            }
//            if (mediaStream.videoTracks.size > 0) {
//                remoteVideoTrack = mediaStream.videoTracks.first()
//                remoteVideoTrack.setEnabled(true)
////                remoteVideoTrack.addSink(svr_remote__call)
////                remoteVideoTrack.addSink {
////                    svr_remote__home.onFrame(VideoFrame(it.buffer, it.rotation + 270, -1))
////                }
//            }

        }

        override fun onDataChannel(dataChannel: DataChannel?) {
            Log.e(TAG, "peerConnectionObserver..onDataChannel")

            dataChannel?.registerObserver(object : DataChannel.Observer {
                override fun onMessage(buffer: DataChannel.Buffer?) {
                    buffer?.let {

                        val bufferData = buffer.data
                        val byteArray = ByteArray(bufferData.remaining())
                        bufferData.get(byteArray)
                        val message = String(byteArray)
                        Log.e(TAG, "message:$message")

                        when (message) {
                            C.SWITCH_CAMERA -> {
                                if (videoCapturer is CameraVideoCapturer) {
                                    val cameraVideoCapturer = videoCapturer as CameraVideoCapturer
                                    cameraVideoCapturer.switchCamera(null)
                                }
                            }
                        }

                    }
                }

                override fun onBufferedAmountChange(p0: Long) {
                }

                override fun onStateChange() {
                    Log.e(
                        TAG,
                        "onStateChange: remote data channel state: " + dataChannel.state()
                            .toString()
                    )
                }

            })
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            Log.e(TAG, "peerConnectionObserver..onIceConnectionReceivingChange")
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            Log.e(TAG, "peerConnectionObserver..onIceConnectionChange:${p0.toString()}")
            when (p0) {
                PeerConnection.IceConnectionState.CONNECTED -> {
                    C.status = C.CONNECTED

                    getAudioFocus()

                    isPeerConnected = true

                }

                PeerConnection.IceConnectionState.CHECKING -> {
                    C.status = C.CONNECTING

                }
                PeerConnection.IceConnectionState.CLOSED -> {
                    C.status = C.DISCONNECTED

                    mHandler.post {
                        disconnectAll()
                    }

                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    C.status = C.DISCONNECTED

                    mHandler.post {
                        disconnectAll()
                    }

                }
                PeerConnection.IceConnectionState.FAILED -> {
                    C.status = C.DISCONNECTED

                    mHandler.post {
                        disconnectAll()
                    }
                }

                else -> {
                }
            }

        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            Log.e(TAG, "peerConnectionObserver..onIceGatheringChange:${p0.toString()}")
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            Log.e(TAG, "peerConnectionObserver..onSignalingChange:${p0.toString()}")
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
            Log.e(TAG, "peerConnectionObserver..onIceCandidatesRemoved")
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
            Log.e(TAG, "peerConnectionObserver..onRemoveStream")
        }

        override fun onRenegotiationNeeded() {
            Log.e(TAG, "peerConnectionObserver..onRenegotiationNeeded")
        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
            Log.e(TAG, "peerConnectionObserver..onAddTrack")
        }

    }

    private var isDisconnecting = false
    fun disconnectAll() {
        Log.e(TAG, "disconnectAll")
        if (!isDisconnecting) {
            isDisconnecting = true

            isPeerConnected = false

            videoCapturer?.let {
                it.stopCapture()
                it.dispose()
                videoCapturer = null
            }

            rootEglBase?.let {
                it.release()
                rootEglBase = null
            }

            peerConnection?.let {
                it.close()
                it.dispose()
                peerConnection = null
            }

            peerConnectionFactory?.let {
                it.dispose()
                peerConnectionFactory = null
            }

            mDatabaseRoom.child(C.cctvId).removeValue()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            audioManager.isSpeakerphoneOn = false

            isDisconnecting = false
        }

    }

    private var audioFocusRequest: AudioFocusRequest? = null
    private fun getAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    build()
                })
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener {}
                build()
            }
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }

        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        audioManager.isSpeakerphoneOn = true

    }
}
