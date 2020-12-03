package com.clearkeep.screen.repo

import com.clearkeep.db.clear_keep.dao.UserDao
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import user.UserGrpc
import user.UserOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
        private val userDao: UserDao,
        private val userStub: UserGrpc.UserBlockingStub,
        private val userManager: UserManager
) {
    suspend fun getProfile() : User?  = withContext(Dispatchers.IO) {
        val existingUser = userDao.getUserByName(userManager.getUserName())
        if (existingUser != null) {
            return@withContext existingUser
        }

        try {
            val request = UserOuterClass.Empty.newBuilder().build()
            val response = userStub.getProfile(request)
            val user = User(response.id, response.username, response.email, response.firstName, response.lastName)
            userDao.save(user)
            userManager.saveClientId(user.id)

            return@withContext user
        } catch (e: Exception) {
            printlnCK("getProfile: $e")
            return@withContext null
        }
    }
}