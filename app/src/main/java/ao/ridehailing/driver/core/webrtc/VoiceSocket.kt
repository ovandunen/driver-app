package ao.ridehailing.driver.core.webrtc

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceSocket @Inject constructor(@ApplicationContext private val context: Context) {
    private var peer: PeerConnection? = null

    fun offer(callId: String): String {
        return try {
            val options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
            PeerConnectionFactory.initialize(options)
            val factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            val track = factory.createAudioTrack("audio-$callId", factory.createAudioSource(MediaConstraints()))
            val created = factory.createPeerConnection(PeerConnection.RTCConfiguration(emptyList()), observer())
            created?.addTrack(track, listOf(callId))
            peer = created
            val future = CompletableFuture<String>()
            created?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription?) {
                    if (description == null) {
                        future.complete("v=0")
                    } else {
                        created.setLocalDescription(ack(), description)
                        future.complete(description.description)
                    }
                }

                override fun onSetSuccess() = Unit

                override fun onCreateFailure(error: String?) {
                    future.complete("v=0")
                }

                override fun onSetFailure(error: String?) = Unit
            }, MediaConstraints())
            future.get(2, TimeUnit.SECONDS)
        } catch (error: Throwable) {
            "v=0"
        }
    }

    fun accept(sdp: String, type: String) {
        val kind = if (type == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        try {
            peer?.setRemoteDescription(ack(), SessionDescription(kind, sdp))
        } catch (error: Throwable) {
            peer = null
        }
    }

    fun stop() {
        try {
            peer?.close()
        } catch (error: Throwable) {
            peer = null
        }
        peer = null
    }

    private fun ack(): SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private fun observer(): PeerConnection.Observer {
        return Proxy.newProxyInstance(
            PeerConnection.Observer::class.java.classLoader,
            arrayOf(PeerConnection.Observer::class.java),
        ) { _, method, _ ->
            if (method.returnType == java.lang.Boolean.TYPE) false else null
        } as PeerConnection.Observer
    }
}
