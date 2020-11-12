package com.clearkeep.login

import com.clearkeep.utilities.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import signal.SignalKeyDistributionGrpc
import javax.inject.Inject
import javax.inject.Singleton

private const val CLIENT_ID = "client_id"

@Singleton
class LoginRepository @Inject constructor(
        private val storage: Storage,
        private val client: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
) {
    fun isUserRegistered() = storage.getString(CLIENT_ID).isNotEmpty()

    fun getClientId() = storage.getString(CLIENT_ID)

    suspend fun register(clientID: String) : Boolean = withContext(Dispatchers.IO) {
        storage.setString(CLIENT_ID, clientID)

        return@withContext true
    }
}