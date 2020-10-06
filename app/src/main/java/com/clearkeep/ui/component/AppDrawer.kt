package com.clearkeep.ui.component

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.state
import androidx.core.content.ContextCompat.startActivity
import androidx.ui.core.Modifier
import androidx.ui.foundation.Image
import androidx.ui.foundation.Text
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.ColorFilter
import androidx.ui.layout.*
import androidx.ui.material.*
import androidx.ui.res.stringResource
import androidx.ui.res.vectorResource
import androidx.ui.unit.dp
import com.clearkeep.clearkeep.R
import com.clearkeep.data.DataStore
import com.clearkeep.db.UserRepository
import com.clearkeep.myapplication.LoginActivity
import com.clearkeep.ui.Screen
import com.clearkeep.ui.navigateTo
import grpc.PscrudGrpc
import grpc.PscrudOuterClass
import io.grpc.stub.StreamObserver

@Composable
fun AppDrawer(
    currentScreen: Screen,
    closeDrawer: () -> Unit,
    activity: Activity,
    dbLocal: UserRepository,
    grpcClient: PscrudGrpc.PscrudStub,mainThreadHandler: Handler
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val openDialogLogout = state { false }
        val stateCloseActivity = state { false }

        Spacer(Modifier.preferredHeight(24.dp))
        DrawerButton(
            icon = R.drawable.ic_home_24dp,
            label = "Rooms",
            isSelected = currentScreen == Screen.Home,
            action = {
                navigateTo(Screen.Home)
                closeDrawer()
            }
        )

        DrawerButton(
            icon = R.drawable.ic_user_24dp,
            label = stringResource(R.string.profile),
            isSelected = currentScreen == Screen.HomeView2,
            action = {
                closeDrawer()
            }
        )

        DrawerButton(
            icon = R.drawable.ic_logout_24dp,
            label = stringResource(R.string.logout),
            isSelected = currentScreen == Screen.HomeView2,
            action = {
                closeDrawer()
                openDialogLogout.value = true
            }
        )

        if (openDialogLogout.value) {
            if (openDialogLogout.value) {
                AlertDialog(
                    onCloseRequest = {
                        openDialogLogout.value = false
                    },
                    title = {
                        Text(text = stringResource(R.string.logout))
                    },
                    text = {
                        Text("Do you want to log out?")
                    },
                    confirmButton = {
                        Button(onClick = {
                            logout(grpcClient,mainThreadHandler,dbLocal ,openDialogLogout,stateCloseActivity )
                        }) {
                            Text("Ok")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            openDialogLogout.value = false
                        }) {
                            Text("Cancel")
                        }
                    },
                    buttonLayout = AlertDialogButtonLayout.SideBySide
                )


            }
        }

        if (stateCloseActivity.value) {
            startActivity(activity, Intent(activity, LoginActivity::class.java), null)
            activity.finish()
        }
    }
}


@Composable
private fun DrawerButton(
    @DrawableRes icon: Int,
    label: String,
    isSelected: Boolean,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colors
    val imageAlpha = if (isSelected) {
        1f
    } else {
        0.6f
    }
    val textIconColor = if (isSelected) {
        colors.primary
    } else {
        colors.onSurface.copy(alpha = 0.6f)
    }
    val backgroundColor = if (isSelected) {
        colors.primary.copy(alpha = 0.12f)
    } else {
        colors.surface
    }

    val surfaceModifier = modifier
        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
        .fillMaxWidth()
    Surface(
        modifier = surfaceModifier,
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                Image(
                    asset = vectorResource(icon),
                    colorFilter = ColorFilter.tint(textIconColor),
                    alpha = imageAlpha
                )
                Spacer(Modifier.preferredWidth(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.body2.copy(color = textIconColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Logout user
fun logout(grpcClient: PscrudGrpc.PscrudStub, mainThreadHandler: Handler,dbLocal: UserRepository ,
           openDialogLogout: MutableState<Boolean>, stateCloseActivity: MutableState<Boolean> ) {
    val request = PscrudOuterClass.Request.newBuilder()
        .setSession(DataStore.session)
        .build()

    grpcClient.logout(request, object : StreamObserver<PscrudOuterClass.Response> {
        override fun onNext(value: PscrudOuterClass.Response?) {
            Log.e("Enc", "logout : " + value?.ok)
            value?.ok?.let { isSuccessful ->
                mainThreadHandler.post{
                    dbLocal.deleteAllUser()
                    openDialogLogout.value = false
                    stateCloseActivity.value = true
                }
            }

        }

        override fun onError(t: Throwable?) {
            Log.e("Enc", "logout onError")

        }

        override fun onCompleted() {
            Log.e("Enc", "logout onCompleted")
        }
    })
}

