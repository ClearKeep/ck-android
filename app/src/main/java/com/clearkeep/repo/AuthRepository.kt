package com.clearkeep.repo

import android.util.Log
import auth.AuthGrpc
import auth.AuthOuterClass
import com.clearkeep.di.CallCredentialsImpl
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Resource
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
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

    suspend fun register(displayName: String, password: String, email: String) : Resource<AuthOuterClass.RegisterRes> = withContext(Dispatchers.IO) {
        printlnCK("register: $displayName, password = $password")
        try {
            val request = AuthOuterClass.RegisterReq.newBuilder()
                    .setDisplayName(displayName)
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
        printlnCK("login: $userName, password = $password")
        try {
            val request = AuthOuterClass.AuthReq.newBuilder()
                    .setEmail(userName)
                    .setPassword(password)
                    .build()
            val response = authBlockingStub.login(request)
            if (response.baseResponse.success) {
                printlnCK("login successfully")
                userManager.saveLoginTime(getCurrentDateTime().time)
                userManager.saveAccessKey(response.accessToken)
                userManager.saveHashKey(response.hashKey)
                userManager.saveRefreshToken(response.refreshToken)
                userManager.saveUserName(userName)
                return@withContext Resource.success(response)
            } else {
                printlnCK("login failed: ${response.baseResponse.errors.message}")
                return@withContext Resource.error(response.baseResponse.errors.message, null)
            }
        } catch (e: Exception) {
            printlnCK("login error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }
    suspend fun loginByGoogle(token:String, userName: String? = ""):Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO){
        try {
            val request=AuthOuterClass
                .GoogleLoginReq
                .newBuilder()
                .setIdToken(token)
                .build()
            val response=authBlockingStub.loginGoogle(request)

            if (response.baseResponse.success) {
                printlnCK("login by google successfully")
                userManager.saveLoginTime(getCurrentDateTime().time)
                userManager.saveAccessKey(response.accessToken)
                userManager.saveHashKey(response.hashKey)
                userManager.saveRefreshToken(response.refreshToken)
                if (userName != null) {
                    userManager.saveUserName(userName)
                }
                return@withContext Resource.success(response)
            }
            return@withContext Resource.success(response)
        } catch (e: Exception) {
            printlnCK("login by google error: $e")
            return@withContext Resource.error(e.toString(), null)
        }

    }

    suspend fun loginByMicrosoft(
        accessToken: String,
        userName: String? = ""
    ): Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        try {
            val request = AuthOuterClass
                .OfficeLoginReq
                .newBuilder()
                .setAccessToken(accessToken)
                .build()
            val response = authBlockingStub.loginOffice(request)
            if (response.baseResponse.success) {
                printlnCK("login by microsoft successfully")
                userManager.saveLoginTime(getCurrentDateTime().time)
                userManager.saveAccessKey(response.accessToken)
                userManager.saveHashKey(response.hashKey)
                userManager.saveRefreshToken(response.refreshToken)
                if (userName != null) {
                    userManager.saveUserName(userName)
                }
                return@withContext Resource.success(response)
            }
            return@withContext Resource.success(response)
        } catch (e: Exception) {
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

    suspend fun logoutFromAPI() : Resource<AuthOuterClass.BaseResponse> = withContext(Dispatchers.IO) {
        printlnCK("logoutFromAPI")
        try {
            val request = AuthOuterClass.LogoutReq.newBuilder()
                    .setDeviceId(userManager.getUniqueDeviceID())
                    .setRefreshToken(userManager.getRefreshToken())
                    .build()
            val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                    .usePlaintext()
                    .executor(Dispatchers.Default.asExecutor())
                    .build()

            val  authBlockingWithHeader = AuthGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10 * 1000, TimeUnit.MILLISECONDS)
                    .withCallCredentials(CallCredentialsImpl(userManager.getAccessKey(), userManager.getHashKey()))
            val response = authBlockingWithHeader
                    .logout(request)
            if (response.success) {
                printlnCK("logoutFromAPI successed")
                return@withContext Resource.success(response)
            } else {
                printlnCK("logoutFromAPI failed: ${response.errors.message}")
                return@withContext Resource.error(response.errors.message, null)
            }
        } catch (e: Exception) {
            printlnCK("logoutFromAPI error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }
}