package com.clearkeep.ui.activity

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.ui.animation.Crossfade
import androidx.ui.core.setContent
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import com.clearkeep.application.MyApplication
import com.clearkeep.data.DataStore
import com.clearkeep.db.UserRepository
import com.clearkeep.model.Room
import com.clearkeep.store.InMemorySignalProtocolStore
import com.clearkeep.ui.ChatStatus
import com.clearkeep.ui.Screen
import com.clearkeep.ui.home.*
import signalc.SignalKeyDistributionGrpc
import signalc.Signalc
import io.grpc.stub.StreamObserver

class MainActivity : AppCompatActivity() {

    lateinit var grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub
    lateinit var mainThreadHandler: Handler
    lateinit var dbLocal: UserRepository
    val rooms = mutableListOf<Room>()
    lateinit var myStore: InMemorySignalProtocolStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as MyApplication).container
        grpcClient = appContainer.grpcClient
        mainThreadHandler = appContainer.mainThreadHandler
        dbLocal = appContainer.dbLocal
        myStore = appContainer.myStore
        subscribe()
        listen()
        setContent {
            DrawerAppComponent()
        }
    }

    @Composable
    fun DrawerAppComponent() {
        Crossfade(ChatStatus.currentScreen) { screen ->
            Surface(color = MaterialTheme.colors.background) {
                when (screen) {
                    is Screen.Home -> HomeView(rooms, this, dbLocal, grpcClient, mainThreadHandler)
                    is Screen.HomeView2 -> HomeView2(this, dbLocal, grpcClient, mainThreadHandler)
                    is Screen.CreateNewRoom -> CreateNewRoom(rooms,dbLocal,grpcClient)
                    is Screen.RoomDetail -> RoomDetail(screen.roomId, grpcClient,myStore, mainThreadHandler)
                }
            }
        }
    }

    // subcribe topic
    private fun subscribe() {
        val request = Signalc.SubscribeAndListenRequest.newBuilder()
            .setClientId(DataStore.username)
            .build()

        grpcClient.subscribe(request, object : StreamObserver<Signalc.BaseResponse> {
            override fun onNext(response: Signalc.BaseResponse?) {
                response?.message?.let { isSuccessful ->
                }
            }

            override fun onError(t: Throwable?) {
            }

            override fun onCompleted() {
            }
        })
    }

    // Listener message from sender
    private fun listen() {
        val request = Signalc.SubscribeAndListenRequest.newBuilder()
            .setClientId(DataStore.username)
            .build()
        grpcClient.listen(request, object : StreamObserver<Signalc.Publication> {
            override fun onNext(value: Signalc.Publication) {
                hear(
                    value.senderId,value.message,
                    myStore,
                    mainThreadHandler
                )
            }

            override fun onError(t: Throwable?) {
                println("onError ${t.toString()}")
            }

            override fun onCompleted() {
                println("onCompleted")
            }
        })
    }

    @Composable
    fun onShowMsg(message: String) {
        mainThreadHandler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

