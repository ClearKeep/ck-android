package com.clearkeep.screen.auth

import auth.AuthGrpc
import auth.AuthOuterClass
import com.clearkeep.screen.chat.repositories.SignalKeyRepository
import com.clearkeep.repository.UserRepository
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
        private val userRepository: UserRepository,
        private val authBlockingStub: AuthGrpc.AuthBlockingStub,
        private val signalKeyRepository: SignalKeyRepository
) {
    fun isUserRegistered() = userRepository.isUserRegistered()

    suspend fun register(userName: String, password: String, email: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("register: $userName")
        try {
            val request = AuthOuterClass.RegisterReq.newBuilder()
                    .setUsername(userName)
                    .setPassword(password)
                    .setEmail(email)
                    .build()
            val response = authBlockingStub.register(request)
            return@withContext response?.success ?: false
        } catch (e: Exception) {
            printlnCK("register error: $e")
            return@withContext false
        }
    }

    suspend fun login(userName: String, password: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("login: $userName")
        try {
            val request = AuthOuterClass.AuthReq.newBuilder()
                    .setUsername(userName)
                    .setPassword(password)
                    .build()
            val response = authBlockingStub.login(request)

            val isRegisterKeySuccess = signalKeyRepository.peerRegisterClientKey(userName)
            if (isRegisterKeySuccess) {
                userRepository.saveAccessKey(response.accessToken)
                userRepository.saveHashKey(response.hashKey)
                userRepository.saveUserName(userName)
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            printlnCK("login error: $e")
            return@withContext false
        }
    }
}