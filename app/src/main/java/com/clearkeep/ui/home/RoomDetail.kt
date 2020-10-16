package com.clearkeep.ui.home

import android.os.Handler
import android.text.TextUtils
import androidx.compose.Composable
import androidx.compose.frames.ModelList
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.IconButton
import androidx.ui.material.Surface
import androidx.ui.material.TopAppBar
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.ArrowBack
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.TextUnit
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.clearkeep.data.DataStore
import com.clearkeep.model.Message
import com.clearkeep.store.InMemorySignalProtocolStore
import com.clearkeep.ui.Screen
import com.clearkeep.ui.navigateTo
import com.clearkeep.ui.widget.HintEditText
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import signalc.SignalKeyDistributionGrpc
import signalc.Signalc
import java.nio.charset.StandardCharsets


private var messagesList = ModelList<Message>()
private var listMsg = mutableMapOf<String, String>()

@Composable
fun RoomDetail(
    roomId: String,
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    mystore: InMemorySignalProtocolStore,
    mainThreadHandler: Handler
) {
    val msgState = state { TextFieldValue("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(text = "Room " + roomId)
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        navigateTo(Screen.Home)
                        messagesList.clear()
                    }
                ) {
                    Icon(asset = Icons.Filled.ArrowBack)
                }
            }
        )
        Surface(color = Color(0xFFfff), modifier = Modifier.weight(1f)) {
            // Center is a composable that centers all the child composables that are passed to it.
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
                        0.66f
                    )
                ) {
                    MessageAdapter()
                }

                Row(
                    verticalGravity = Alignment.CenterVertically
                ) {

                    Surface(
                        color = Color.LightGray,
                        modifier = Modifier.padding(8.dp)
                                + Modifier.weight(0.66f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        HintEditText(
                            hintText = "Next Message",
                            modifier = Modifier.padding(16.dp) + Modifier.fillMaxWidth(),
                            textValue = msgState
                        )
                    }
                    Button(
                        modifier = Modifier.padding(8.dp),
                        onClick = {
                            if (!TextUtils.isEmpty(msgState.value.text)) {
                                sendMessage(roomId,
                                    msgState.value.text,
                                    grpcClient,
                                    mystore
                                )
                                // update message of sender to UI
                                messagesList.add(
                                    Message(
                                        roomId, DataStore.username + " : " + msgState.value.text
                                    )
                                )
                                // clear text
                                msgState.value = TextFieldValue("")
                            }
                        }
                    ) {
                        Text(
                            text = "Send",
                            style = TextStyle(fontSize = TextUnit.Sp(16))
                        )
                    }
                }
            }

        }
    }
}

@Preview
@Composable
fun previewScreen() {
    val msgState = state { TextFieldValue("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(text = "Room ")
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        navigateTo(Screen.Home)
                        messagesList.clear()
                    }
                ) {
                    Icon(asset = Icons.Filled.ArrowBack)
                }
            }
        )
        Surface(color = Color(0xFFfff), modifier = Modifier.weight(1f)) {
            // Center is a composable that centers all the child composables that are passed to it.
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
                        0.66f
                    )
                ) {
                    MessageAdapter()
                }

                Row() {

                    Surface(
                        color = Color.LightGray,
                        modifier = Modifier.padding(8.dp)
                                + Modifier.weight(0.66f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        HintEditText(
                            hintText = "Next Message",
                            modifier = Modifier.padding(16.dp) + Modifier.fillMaxWidth(),
                            textValue = msgState
                        )
                    }
                    Button(
                        modifier = Modifier.padding(8.dp),
                        onClick = {

                        }
                    ) {
                        Text(
                            text = "Send",
                            style = TextStyle(fontSize = TextUnit.Sp(16))
                        )
                    }
                }
            }

        }
    }
}

private fun sendMessage(receiver: String, msg: String, grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
                        myStore: InMemorySignalProtocolStore) {
    if (TextUtils.isEmpty(receiver)) {
        return
    }
    val requestUser = Signalc.SignalKeysUserRequest.newBuilder()
        .setClientId(receiver)
        .build()
    grpcClient.getKeyBundleByUserId(requestUser, object :
        StreamObserver<Signalc.SignalKeysUserResponse> {
        override fun onNext(value: Signalc.SignalKeysUserResponse) {
            val preKey = PreKeyRecord(value.preKey.toByteArray())
            val signedPreKey = SignedPreKeyRecord(value.signedPreKey.toByteArray())
            val identityKeyPublic = IdentityKey(value.identityKeyPublic.toByteArray(), 0)

            val retrievedPreKey = PreKeyBundle(
                value.registrationId,
                value.deviceId,
                preKey.id,
                preKey.keyPair.publicKey,
                value.signedPreKeyId,
                signedPreKey.keyPair.publicKey,
                signedPreKey.signature,
                identityKeyPublic
            )

            val signalProtocolAddress = SignalProtocolAddress(receiver, 111)

            val sessionBuilder = SessionBuilder(myStore, signalProtocolAddress)

            // Build a session with a PreKey retrieved from the server.
            sessionBuilder.process(retrievedPreKey)

            val sessionCipher = SessionCipher(myStore, signalProtocolAddress)
            val message: CiphertextMessage =
                sessionCipher.encrypt(msg.toByteArray(charset("UTF-8")))

            val request = Signalc.PublishRequest.newBuilder()
                .setReceiveId(receiver)
                .setSenderId(DataStore.username)
                .setMessage(ByteString.copyFrom(message.serialize()))
                .build()

            grpcClient.publish(request, object : StreamObserver<Signalc.BaseResponse> {
                override fun onNext(response: Signalc.BaseResponse?) {
                    println("Test, onNext ${response?.message}")
                }

                override fun onError(t: Throwable?) {
                    println("Test, onError ${t?.message}")
                }

                override fun onCompleted() {
                    println("Test, onCompleted")
                }
            })
        }

        override fun onError(t: Throwable?) {
            println("Test, onError ${t?.message}")
        }

        override fun onCompleted() {
            println("Test, onCompleted")
        }
    })
}

fun hear(
    senderId: String,
    msg: ByteString,
    myStore: InMemorySignalProtocolStore,
    mainThreadHandler: Handler
) {
    try {
        val signalProtocolAddress = SignalProtocolAddress(senderId, 111)
        val preKeyMessage = PreKeySignalMessage(msg.toByteArray())
        val sessionCipher = SessionCipher(myStore, signalProtocolAddress)
        val message = sessionCipher.decrypt(preKeyMessage)
        val result = String(message, StandardCharsets.UTF_8)
        mainThreadHandler.post(Runnable {
            messagesList.add(
                Message(
                    senderId, senderId + " : " + result
                )
            )
        })

        println(result)
    } catch (e: Exception) {
        println(e.toString())
    }
}

@Composable
fun MessageAdapter() {
    VerticalScroller {
        Column(modifier = Modifier.fillMaxWidth()) {
            messagesList.forEach {
                Column(
                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp)
                            + Modifier.fillMaxWidth()
                ) {
                    Text(
                        it.message, style = TextStyle(
                            color = Color.Black,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}




