package com.clearkeep.ui.home

import android.os.Handler
import android.text.TextUtils
import android.util.Log
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
import com.clearkeep.db.UserRepository
import com.clearkeep.model.Message
import com.clearkeep.secure.CryptoHelper
import com.clearkeep.ui.Screen
import com.clearkeep.ui.navigateTo
import com.clearkeep.ui.widget.HintEditText
import com.google.protobuf.ByteString
import grpc.SignalKeyDistributionGrpc


private var messagesList = ModelList<Message>()
private var listMsg = mutableMapOf<String, String>()

@Composable
fun RoomDetail(
    roomId: String,
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
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
                                sendMsg(grpcClient, msgState.value.text, roomId, mainThreadHandler)
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


fun hear(
    id: String,
    chit: ByteString,
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    mainThreadHandler: Handler, dbLocal: UserRepository
) {
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

fun sendMsg(
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    message: String,
    recipient: String,
    mainThreadHandler: Handler
) {
    if (TextUtils.isEmpty(message)) {
        return
    }
    if (CryptoHelper.checkHandShaked(recipient)) {
        Log.e("Enc", "Send Message imtermadiate")
        sendMessage(message, recipient, grpcClient, mainThreadHandler)
    } else {
        Log.e("Enc", "Send Handshake")
        listMsg.put(recipient, message)
        // send handShake
        sendHandshake(grpcClient, recipient, mainThreadHandler)
    }

}

private fun sendHandshake(
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    recipient: String,
    mainThreadHandler: Handler
) {

}

private fun sendConfirmHandshake(
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    keyConfirm: ByteArray,
    senderID: String,
    mainThreadHandler: Handler
) {

}

private fun sendDataAfterHandshake(
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    recipient: String,
    data: ByteString,
    mainThreadHandler: Handler
) {


}



private fun  sendMessage(
    msgSend: String,
    peer: String, grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    mainThreadHandler: Handler
) {
    val keySet = CryptoHelper.getKeySet(peer)
    val secret = keySet?.let {
        CryptoHelper.getSecretKey(it)
    }

}

private fun sendData(
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    recipient: String,
    data: ByteString,
    mainThreadHandler: Handler
) {


}

