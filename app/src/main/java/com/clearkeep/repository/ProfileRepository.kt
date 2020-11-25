package com.clearkeep.repository

import com.clearkeep.db.UserDao
import com.clearkeep.db.model.User
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserGrpc
import user.UserOuterClass
import javax.inject.Inject
import javax.inject.Singleton

private const val CLIENT_ID = "client_id"

@Singleton
class ProfileRepository @Inject constructor(
        private val storage: Storage,
        private val userDao: UserDao,
        private val userStub: UserGrpc.UserBlockingStub,
        private val userManager: UserManager
) {
    suspend fun updateProfile() : User?  = withContext(Dispatchers.IO) {
        val existingUser = getProfile()
        if (existingUser != null) {
            return@withContext existingUser
        }

        try {
            val request = UserOuterClass.Empty.newBuilder().build()
            val response = userStub.getProfile(request)
            val user = User(response.id, response.username, response.email, response.firstName, response.lastName)
            userDao.save(user)
            saveClientId(user.id)

            return@withContext user
        } catch (e: Exception) {
            printlnCK("getProfile: $e")
            return@withContext null
        }
    }

    private fun saveClientId(clientId: String) {
        storage.setString(CLIENT_ID, clientId)
    }

    fun getClientId() : String {
        return storage.getString(CLIENT_ID)
    }

    suspend fun getProfile(): User {
        return userDao.getUserByName(userManager.getUserName())
    }
}