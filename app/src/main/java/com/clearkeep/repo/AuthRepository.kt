package com.clearkeep.repo

import auth.AuthOuterClass
import com.clearkeep.dynamicapi.CallCredentialsImpl
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userManager: UserManager,
    private val dynamicAPIProvider: DynamicAPIProvider,
) {
    suspend fun register(displayName: String, password: String, email: String, domain: String) : Resource<AuthOuterClass.RegisterRes> = withContext(Dispatchers.IO) {
        printlnCK("register: $displayName, password = $password, domain = $domain")
        dynamicAPIProvider.setUpDomain(domain)
        try {
            val request = AuthOuterClass.RegisterReq.newBuilder()
                    .setDisplayName(displayName)
                    .setPassword(password)
                    .setEmail(email)
                .setWorkspaceDomain(domain)
                    .build()
            val response = dynamicAPIProvider.provideAuthBlockingStub().register(request)
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

    suspend fun login(userName: String, password: String, domain: String) : Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        printlnCK("login: $userName, password = $password, domain = $domain")
        dynamicAPIProvider.setUpDomain(domain)
        try {
            val request = AuthOuterClass.AuthReq.newBuilder()
                    .setEmail(userName)
                    .setPassword(password)
                .setWorkspaceDomain(domain)
                    .build()
            val response = dynamicAPIProvider.provideAuthBlockingStub().login(request)
            if (response.baseResponse.success) {
                printlnCK("login successfully")
                userManager.saveLoginTime(getCurrentDateTime().time)
                userManager.saveAccessKey(response.accessToken)
                userManager.saveHashKey(response.hashKey)
                userManager.saveRefreshToken(response.refreshToken)
                userManager.saveDomainUrl(domain)
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
    suspend fun loginByGoogle(token:String, domain: String):Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO){
        printlnCK("loginByGoogle: token = $token, domain = $domain")
        dynamicAPIProvider.setUpDomain(domain)
        try {
            val request=AuthOuterClass
                .GoogleLoginReq
                .newBuilder()
                .setIdToken(token)
                .setWorkspaceDomain(domain)
                .build()
            val response=dynamicAPIProvider.provideAuthBlockingStub().loginGoogle(request)

            if (response.baseResponse.success) {
                printlnCK("login by google successfully")
                userManager.saveLoginTime(getCurrentDateTime().time)
                userManager.saveAccessKey(response.accessToken)
                userManager.saveHashKey(response.hashKey)
                userManager.saveRefreshToken(response.refreshToken)
                userManager.saveDomainUrl(domain)
                return@withContext Resource.success(response)
            }
            return@withContext Resource.error(response.baseResponse.errors.message, null)
        } catch (e: Exception) {
            printlnCK("login by google error: $e")
            return@withContext Resource.error(e.toString(), null)
        }

    }

    suspend fun loginByFacebook(token:String, domain: String):Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO){
        dynamicAPIProvider.setUpDomain(domain)
        try {
            val request=AuthOuterClass
                .FacebookLoginReq
                .newBuilder()
                .setAccessToken(token)
                .setWorkspaceDomain(domain)
                .build()
            val response=dynamicAPIProvider.provideAuthBlockingStub().loginFacebook(request)

            if (response.baseResponse.success) {
                printlnCK("login by facebook successfully: $response")
                userManager.saveLoginTime(getCurrentDateTime().time)
                userManager.saveAccessKey(response.accessToken)
                userManager.saveHashKey(response.hashKey)
                userManager.saveRefreshToken(response.refreshToken)
                userManager.saveDomainUrl(domain)
                return@withContext Resource.success(response)
            }
            return@withContext Resource.error(response.baseResponse.errors.message, null)
        } catch (e: Exception) {
            printlnCK("login by facebook error: $e")
            return@withContext Resource.error(e.toString(), null)
        }

    }

    suspend fun loginByMicrosoft(
        accessToken: String,
        domain: String
    ): Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO) {
        dynamicAPIProvider.setUpDomain(domain)
        try {
            val request = AuthOuterClass
                .OfficeLoginReq
                .newBuilder()
                .setAccessToken(accessToken)
                .setWorkspaceDomain(domain)
                .build()
            val response = dynamicAPIProvider.provideAuthBlockingStub().loginOffice(request)
            if (response.baseResponse.success) {
                printlnCK("login by microsoft successfully")
                userManager.saveLoginTime(getCurrentDateTime().time)
                userManager.saveAccessKey(response.accessToken)
                userManager.saveHashKey(response.hashKey)
                userManager.saveRefreshToken(response.refreshToken)
                userManager.saveDomainUrl(domain)
                return@withContext Resource.success(response)
            }
            return@withContext Resource.error(response.baseResponse.errors.message, null)
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
            val response = dynamicAPIProvider.provideAuthBlockingStub().fogotPassword(request)
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

            val  authBlockingWithHeader = dynamicAPIProvider.provideAuthBlockingStub()
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