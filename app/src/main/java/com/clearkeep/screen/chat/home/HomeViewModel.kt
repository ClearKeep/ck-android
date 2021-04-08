package com.clearkeep.screen.chat.home

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.R
import com.clearkeep.db.ClearKeepDatabase
import com.clearkeep.db.SignalKeyDatabase
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.repo.*
import com.clearkeep.utilities.FIREBASE_TOKEN
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.storage.UserPreferencesStorage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import io.grpc.ManagedChannel
import kotlinx.coroutines.*
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,

    private val signalProtocolStore: InMemorySignalProtocolStore,

    private val storage: UserPreferencesStorage,
    private val clearKeepDatabase: ClearKeepDatabase,
    private val signalKeyDatabase: SignalKeyDatabase,

    private val managedChannel: ManagedChannel,
): ViewModel() {
    private val _isLogOutProcessing = MutableLiveData<Boolean>()
    lateinit var googleSignIn: GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient

    val isLogOutProcessing: LiveData<Boolean>
        get() = _isLogOutProcessing

    private val _isLogOutCompleted = MutableLiveData(false)

    val isLogOutCompleted: LiveData<Boolean>
        get() = _isLogOutCompleted

    fun networkAvailable() {
        /*if (chatRepository.isNeedSubscribeAgain()) {
            viewModelScope.launch {
                chatRepository.reInitSubscribe()
                updateNewMessages()
            }
        }*/
        managedChannel.resetConnectBackoff()
    }

    fun updateFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                printlnCK("Fetching FCM registration token failed, ${task.exception}")
            }

            // Get new FCM registration token
            val token = task.result
            if (!token.isNullOrEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    storage.setString(FIREBASE_TOKEN, token)
                    pushFireBaseTokenToServer()
                }
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _isLogOutProcessing.value = true

            authRepository.logoutFromAPI()
            clearDatabase()
            _isLogOutCompleted.value = true
        }
    }

    fun logOutGoogle(activity: Activity, onComplete: (() -> Unit)) {
        googleSignInClient.signOut()
            .addOnCompleteListener(activity) {
                Log.e("HomeViewModel","antx logOutGoogle ${it.result.toString()}")
            }
    }

    private suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        storage.clear()
        signalProtocolStore.clear()
        clearKeepDatabase.clearAllTables()
        signalKeyDatabase.clearAllTables()
    }

    private suspend fun pushFireBaseTokenToServer() = withContext(Dispatchers.IO) {
        val token = storage.getString(FIREBASE_TOKEN)
        if (!token.isNullOrEmpty()) {
            printlnCK("push token  = $token ")
            profileRepository.registerToken(token)
        }
    }

    fun initGoogleSignIn(context: Context){
        googleSignIn= GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestProfile()
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, googleSignIn)
    }

}
