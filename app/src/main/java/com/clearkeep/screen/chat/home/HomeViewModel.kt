package com.clearkeep.screen.chat.home

import android.app.Activity
import android.content.Context
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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
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

    val isLogOutProcessing: LiveData<Boolean>
        get() = _isLogOutProcessing

    private val _isLogOutCompleted = MutableLiveData(false)

    val isLogOutCompleted: LiveData<Boolean>
        get() = _isLogOutCompleted

    var mSingleAccountApp: ISingleAccountPublicClientApplication? = null

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
        val googleSignIn = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
        GoogleSignIn.getClient(activity, googleSignIn)
            .signOut().addOnCompleteListener(activity) {
                printlnCK("google sign out  = ${it.result.toString()} ")
                onComplete.invoke()
            }
    }

    private fun getSingleAccountMicrosoft(context: Context, onSuccess: (()->Unit)){
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context, R.raw.auth_config_single_account, object :
                IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
                    mSingleAccountApp = application
                    onSuccess.invoke()
                }

                override fun onError(exception: MsalException?) {
                    printlnCK("Init MicrosoftSignIn error message: ${exception?.message}")
                }
            })
    }

    fun onLogOutMicrosoft(activity: Activity){
        getSingleAccountMicrosoft(activity) {
            mSingleAccountApp?.signOut(object :
                ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    printlnCK("microsoft sign out success")
                }

                override fun onError(exception: MsalException) {
                    printlnCK("microsoft sign out error message: ${exception.message}")
                }
            })
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
}
