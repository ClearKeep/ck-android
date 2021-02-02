package com.clearkeep.screen.auth

import auth.AuthGrpc
import auth.AuthOuterClass
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userManager: UserManager,
    private val authBlockingStub: AuthGrpc.AuthBlockingStub,
) {
    /*suspend fun register(userName: String, password: String, email: String) : Boolean = withContext(Dispatchers.IO) {
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
    }*/

    suspend fun register(userName: String, password: String, email: String) : Resource<AuthOuterClass.RegisterRes> = withContext(Dispatchers.IO) {
        printlnCK("register: $userName")
        try {
            val request = AuthOuterClass.RegisterReq.newBuilder()
                    .setDisplayName(userName)
                    .setPassword(password)
                    .setEmail(email)
                    .build()
            val response = authBlockingStub.register(request)
            printlnCK("register failed: ${response.baseResponse.success}")
            if (response.baseResponse.success) {
                return@withContext Resource.success(response)
            } else {
                printlnCK("register failed: ${response.baseResponse.errors.message}")
                return@withContext Resource.error(response.baseResponse.errors.message, null)
            }
        } catch (e: Exception) {
            printlnCK("register error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    /*suspend fun login(userName: String, password: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("login: $userName")
        try {
            val request = AuthOuterClass.AuthReq.newBuilder()
                    .setUsername(userName)
                    .setPassword(password)
                    .build()
            val response = authBlockingStub.login(request)
            userManager.saveAccessKey(response.accessToken)
            userManager.saveHashKey(response.hashKey)
            userManager.saveUserName(userName)

            return@withContext true
        } catch (e: Exception) {
            printlnCK("login error: $e")
            return@withContext false
        }
    }*/

    suspend fun login(userName: String, password: String) : Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        printlnCK("login: $userName")
        try {
            val request = AuthOuterClass.AuthReq.newBuilder()
                    .setEmail(userName)
                    .setPassword(password)
                    .build()
            val response = authBlockingStub.login(request)
            if (response.baseResponse.success) {
                printlnCK("login successfully")
                userManager.saveAccessKey(response.accessToken)
                userManager.saveHashKey(response.hashKey)
                userManager.saveUserName(userName)
                return@withContext Resource.success(response)
            } else {
                printlnCK("login failed: ${response.baseResponse.errors.message}")
                return@withContext Resource.error(response.baseResponse.errors.message, null)
            }
        } catch (e: Exception) {
            printlnCK("login error: $e")
            if (e.message?.contains("1001") == true) {
                return@withContext Resource.error("Please check username and pass again", null)
            }
            return@withContext Resource.error(e.toString(), null)
        }
    }

    suspend fun recoverPassword(email: String) : Resource<AuthOuterClass.BaseResponse> = withContext(Dispatchers.IO) {
        printlnCK("recoverPassword: $email")
        try {
            val request = AuthOuterClass.FogotPassWord.newBuilder()
                    .setEmail(email)
                    .build()
            val response = authBlockingStub.fogotPassword(request)
            if (response.success) {
                return@withContext Resource.success(response)
            } else {
                return@withContext Resource.error(response.errors.message, null)
            }
        } catch (e: Exception) {
            printlnCK("recoverPassword error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }
}