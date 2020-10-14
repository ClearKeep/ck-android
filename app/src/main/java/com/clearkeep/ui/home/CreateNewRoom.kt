package com.clearkeep.ui.home

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.*
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.*
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.ArrowBack
import androidx.ui.res.imageResource
import androidx.ui.unit.dp
import com.clearkeep.ck.R
import com.clearkeep.db.UserRepository
import com.clearkeep.model.Room
import com.clearkeep.state.backpress.BackButtonHandler
import com.clearkeep.ui.Screen
import com.clearkeep.ui.navigateTo
import com.clearkeep.ui.widget.FilledTextInputComponent
import signalc.SignalKeyDistributionGrpc
import signalc.Signalc
import io.grpc.stub.StreamObserver

@Composable
fun CreateNewRoom(
    rooms: MutableList<Room>,
    dbLocal: UserRepository,
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(text = "Create a New Room")
            },
            navigationIcon = {
                IconButton(onClick = { navigateTo(Screen.Home) }) {
                    Icon(asset = Icons.Filled.ArrowBack)
                }
            }
        )
        Surface(color = Color(0xFFfff), modifier = Modifier.weight(1f)) {
            // Center is a composable that centers all the child composables that are passed to it.
            viewCreateNewRoom(rooms, dbLocal, grpcClient)
        }
    }
    // Event back press on the device
    BackButtonHandler {
        navigateTo(Screen.Home)
    }
}

@Composable
fun viewCreateNewRoom(
    rooms: MutableList<Room>,
    dbLocal: UserRepository,
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub
) {
    val roomId = state { "" }
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.preferredHeight(24.dp))

            val image = imageResource(R.drawable.door)
            val imageModifier = Modifier
                .preferredSize(100.dp)

            Box(Modifier.fillMaxWidth(), gravity = Alignment.TopCenter) {
                Image(image, imageModifier)
            }
            Spacer(Modifier.preferredHeight(48.dp))

            FilledTextInputComponent(
                "Recipient",
                " ",
                roomId
            )

            Spacer(Modifier.preferredHeight(48.dp))
            Row() {

                OutlinedButton(
                    onClick = {
                        if (roomId.value.isNotBlank()) {
                            createRoomID(roomId.value, rooms, dbLocal, grpcClient)
                        }
                    },
                    modifier = Modifier.padding(16.dp) + Modifier.weight(1f)
                ) {
                    Text(
                        text = "Ok",
                        color = Color.Blue,
                        modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp)
                    )
                }


                OutlinedButton(
                    onClick = { navigateTo(Screen.Home) },
                    modifier = Modifier.padding(16.dp) + Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.Blue,
                        modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp)
                    )
                }
            }
        }
    }

}
fun createRoomID(
    roomId: String,
    rooms: MutableList<Room>,
    dbLocal: UserRepository,
    grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub
) {
    val request = Signalc.SignalKeysUserRequest.newBuilder()
        .setClientId(dbLocal.allUser()?.get(0)?.firstName)
        .build()
    grpcClient.getKeyBundleByUserId(
        request,
        object : StreamObserver<Signalc.SignalKeysUserResponse> {
            override fun onNext(response: Signalc.SignalKeysUserResponse?) {
                rooms.add(Room(roomId))
                navigateTo(Screen.Home)
            }

            override fun onError(t: Throwable?) {
            }

            override fun onCompleted() {
            }
        })
}