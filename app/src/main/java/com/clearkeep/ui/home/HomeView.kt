package com.clearkeep.ui.home

import android.app.Activity
import android.os.Handler
import androidx.compose.*
import androidx.ui.core.Modifier
import androidx.ui.foundation.*
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.ColumnScope.weight
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.material.*
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Menu
import androidx.ui.material.ripple.ripple
import androidx.ui.res.imageResource
import androidx.ui.res.loadVectorResource
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.clearkeep.clearkeep.R
import com.clearkeep.data.DataStore
import com.clearkeep.db.UserRepository
import com.clearkeep.model.Room
import com.clearkeep.ui.Screen
import com.clearkeep.ui.component.AppDrawer
import com.clearkeep.ui.navigateTo
import grpc.PscrudGrpc

@Composable
fun HomeView(
    rooms: List<Room>, activity: Activity,
    dbLocal: UserRepository, grpcClient: PscrudGrpc.PscrudStub,mainThreadHandler: Handler,
    scaffoldState: ScaffoldState = remember { ScaffoldState() }
) {
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            AppDrawer(
                currentScreen = Screen.Home,
                closeDrawer = { scaffoldState.drawerState = DrawerState.Closed },
                activity = activity,
                dbLocal = dbLocal,
                grpcClient=grpcClient,mainThreadHandler = mainThreadHandler
            )
        },
        topAppBar = {
            TopAppBar(
                title = { Text(text = "Rooms with acc: " + DataStore.username) },
                navigationIcon = {
                    IconButton(onClick = { scaffoldState.drawerState = DrawerState.Opened }) {
                        Icon(asset = Icons.Filled.Menu)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navigateTo(Screen.CreateNewRoom)
                    }) {
                        val vectorAsset = loadVectorResource(R.drawable.ic_add_white_24dp)
                        vectorAsset.resource.resource?.let {
                            Image(
                                asset = it
                            )
                        }
                    }
                }
            )
        },
        bodyContent = { modifier ->
            Surface(color = Color.White, modifier = Modifier.weight(1f)) {
                AdapterRoomListing(rooms)
            }
        }
    )

}

@Composable
fun SendImage() {
    val image = imageResource(R.drawable.door)
    val imageModifier = Modifier
        .preferredSize(100.dp)
}

@Composable
fun FilledTextInputComponent() {
    var textValue by state { TextFieldValue("") }
    FilledTextField(
        value = textValue,
        onValueChange = { textValue = it },
        label = { Text("Enter Your Name") },
        placeholder = { Text(text = "John Doe") },
        modifier = Modifier.padding(16.dp) + Modifier.fillMaxWidth()
    )
}

@Composable
fun AdapterRoomListing(rooms: List<Room>) {
    AdapterList(data = rooms, modifier = Modifier.weight(1f)) { room ->
        val index = rooms.indexOf(room)
        Clickable(
            onClick = { onRowClick(room.id) },
            modifier = Modifier.ripple()
        ) {
            Column(
                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp)
                        + Modifier.fillMaxWidth()
            ) {
                Text(
                    room.id, style = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    ), modifier = Modifier.padding(12.dp)
                )
                Divider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }
    }
}

fun onRowClick(roomId: String) {
    navigateTo(Screen.RoomDetail(roomId))
}