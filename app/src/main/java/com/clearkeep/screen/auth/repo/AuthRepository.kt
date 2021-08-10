package com.clearkeep.screen.auth.repo

import auth.AuthOuterClass
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.db.clear_keep.model.Profile
import com.clearkeep.db.signal_key.CKSignalProtocolAddress
import com.clearkeep.dynamicapi.CallCredentials
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.SignalKeyRepository
import com.clearkeep.screen.chat.repo.UserPreferenceRepository
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Resource
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import signal.Signal
import signal.SignalKeyDistributionGrpc
import user.UserGrpc
import user.UserOuterClass
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    // network
    private val paramAPIProvider: ParamAPIProvider,

    private val userManager: AppStorage,
    private val serverRepository: ServerRepository,
    private val myStore: InMemorySignalProtocolStore,
    private val signalKeyRepository: SignalKeyRepository,
    private val userPreferenceRepository: UserPreferenceRepository
) {
    suspend fun register(displayName: String, password: String, email: String, domain: String) : Resource<AuthOuterClass.RegisterRes> = withContext(Dispatchers.IO) {
        printlnCK("register: $displayName, password = $password, domain = $domain")
        try {
            val request = AuthOuterClass.RegisterReq.newBuilder()
                    .setDisplayName(displayName)
                    .setPassword(password)
                    .setEmail(email)
                .setWorkspaceDomain(domain)
                    .build()
            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).register(request)
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
        try {
            val request = AuthOuterClass.AuthReq.newBuilder()
                    .setEmail(userName)
                    .setPassword(password)
                .setWorkspaceDomain(domain)
                    .build()
            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).login(request)
            if (response.baseResponse.success) {
                printlnCK("login successfully")
                val accessToken = response.accessToken
                val hashKey = response.hashKey
                val profile = getProfile(paramAPIProvider.provideUserBlockingStub(ParamAPI(domain, accessToken, hashKey)))
                    ?: return@withContext Resource.error("Can not get profile", null)
                val isRegisterKeySuccess = peerRegisterClientKeyWithGrpc(
                    Owner(domain, profile.userId),
                    paramAPIProvider.provideSignalKeyDistributionBlockingStub(ParamAPI(domain, accessToken, hashKey))
                )
                if (!isRegisterKeySuccess) {
                    return@withContext Resource.error("Can not register key", null)
                }

                serverRepository.insertServer(
                    Server(
                        serverName = response.workspaceName,
                        serverDomain = domain,
                        ownerClientId = profile.userId,
                        serverAvatar = "",
                        loginTime = getCurrentDateTime().time,
                        accessKey = accessToken,
                        hashKey = hashKey,
                        refreshToken = response.refreshToken,
                        profile = profile,
                    )
                )
                userPreferenceRepository.initDefaultUserPreference(domain, profile.userId)
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
        try {
            val request=AuthOuterClass
                .GoogleLoginReq
                .newBuilder()
                .setIdToken(token)
                .setWorkspaceDomain(domain)
                .build()
            val response=paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).loginGoogle(request)

            if (response.baseResponse.success) {
                printlnCK("login by google successfully")
                val accessToken = response.accessToken
                val hashKey = response.hashKey
                val profile = getProfile(paramAPIProvider.provideUserBlockingStub(ParamAPI(domain, accessToken, hashKey)))
                    ?: return@withContext Resource.error("Can not get profile", null)
                val isRegisterKeySuccess = peerRegisterClientKeyWithGrpc(
                    Owner(domain, profile.userId),
                    paramAPIProvider.provideSignalKeyDistributionBlockingStub(ParamAPI(domain, accessToken, hashKey))
                )
                if (!isRegisterKeySuccess) {
                    return@withContext Resource.error("Can not register key", null)
                }

                serverRepository.insertServer(
                    Server(
                        serverName = response.workspaceName,
                        serverDomain = domain,
                        ownerClientId = profile.userId,
                        serverAvatar = "",
                        loginTime = getCurrentDateTime().time,
                        accessKey = accessToken,
                        hashKey = hashKey,
                        refreshToken = response.refreshToken ?: "",
                        profile = profile,
                    )
                )
                userPreferenceRepository.initDefaultUserPreference(domain, profile.userId)
                return@withContext Resource.success(response)
            }
            return@withContext Resource.error(response.baseResponse.errors.message, null)
        } catch (e: Exception) {
            printlnCK("login by google error: $e")
            return@withContext Resource.error(e.toString(), null)
        }

    }

    suspend fun loginByFacebook(token:String, domain: String):Resource<AuthOuterClass.AuthRes> = withContext(Dispatchers.IO){
        try {
            val request=AuthOuterClass
                .FacebookLoginReq
                .newBuilder()
                .setAccessToken(token)
                .setWorkspaceDomain(domain)
                .build()
            val response= paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).loginFacebook(request)

            if (response.baseResponse.success) {
                printlnCK("login by facebook successfully: $response")
                val accessToken = response.accessToken
                val hashKey = response.hashKey
                val profile = getProfile(paramAPIProvider.provideUserBlockingStub(ParamAPI(domain, accessToken, hashKey)))
                    ?: return@withContext Resource.error("Can not get profile", null)
                val isRegisterKeySuccess = peerRegisterClientKeyWithGrpc(
                    Owner(domain, profile.userId),
                    paramAPIProvider.provideSignalKeyDistributionBlockingStub(ParamAPI(domain, accessToken, hashKey))
                )
                if (!isRegisterKeySuccess) {
                    return@withContext Resource.error("Can not register key", null)
                }

                serverRepository.insertServer(
                    Server(
                        serverName = response.workspaceName,
                        serverDomain = domain,
                        ownerClientId = profile.userId,
                        serverAvatar = "",
                        loginTime = getCurrentDateTime().time,
                        accessKey = accessToken,
                        hashKey = hashKey,
                        refreshToken = response.refreshToken,
                        profile = profile,
                    )
                )
                userPreferenceRepository.initDefaultUserPreference(domain, profile.userId)
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
        try {
            val request = AuthOuterClass
                .OfficeLoginReq
                .newBuilder()
                .setAccessToken(accessToken)
                .setWorkspaceDomain(domain)
                .build()
            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).loginOffice(request)
            if (response.baseResponse.success) {
                printlnCK("login by microsoft successfully")

                val accessToken = response.accessToken
                val hashKey = response.hashKey
                val profile = getProfile(paramAPIProvider.provideUserBlockingStub(ParamAPI(domain, accessToken, hashKey)))
                    ?: return@withContext Resource.error("Can not get profile", null)
                val isRegisterKeySuccess = peerRegisterClientKeyWithGrpc(
                    Owner(domain, profile.userId),
                    paramAPIProvider.provideSignalKeyDistributionBlockingStub(ParamAPI(domain, accessToken, hashKey))
                )
                if (!isRegisterKeySuccess) {
                    return@withContext Resource.error("Can not register key", null)
                }

                serverRepository.insertServer(
                    Server(
                        serverName = response.workspaceName,
                        serverDomain = domain,
                        ownerClientId = profile.userId,
                        serverAvatar = "",
                        loginTime = getCurrentDateTime().time,
                        accessKey = accessToken,
                        hashKey = hashKey,
                        refreshToken = response.refreshToken,
                        profile = profile,
                    )
                )
                userPreferenceRepository.initDefaultUserPreference(domain, profile.userId)
                return@withContext Resource.success(response)
            }
            return@withContext Resource.error(response.baseResponse.errors.message, null)
        } catch (e: Exception) {
            return@withContext Resource.error(e.toString(), null)
        }
    }

    suspend fun recoverPassword(email: String, domain: String) : Resource<AuthOuterClass.BaseResponse> = withContext(Dispatchers.IO) {
        printlnCK("recoverPassword: $email")
        try {
            val request = AuthOuterClass.FogotPassWord.newBuilder()
                    .setEmail(email)
                    .build()
            val response = paramAPIProvider.provideAuthBlockingStub(ParamAPI(domain)).fogotPassword(request)
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

    suspend fun logoutFromAPI(server: Server) : Resource<AuthOuterClass.BaseResponse> = withContext(Dispatchers.IO) {
        printlnCK("logoutFromAPI")
        try {
            val request = AuthOuterClass.LogoutReq.newBuilder()
                    .setDeviceId(userManager.getUniqueDeviceID())
                    .setRefreshToken("")
                    .build()

            val  authBlockingWithHeader = paramAPIProvider.provideAuthBlockingStub(ParamAPI(server.serverDomain))
                    .withDeadlineAfter(10 * 1000, TimeUnit.MILLISECONDS)
                    .withCallCredentials(CallCredentials(server.accessKey, server.hashKey))
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

    private suspend fun getProfile(userGrpc: UserGrpc.UserBlockingStub) : Profile?  = withContext(Dispatchers.IO) {
        try {
            val request = UserOuterClass.Empty.newBuilder().build()
            val response = userGrpc.getProfile(request)
            printlnCK("getProfileWithGrpc: $response")
            return@withContext Profile(
                userId = response.id,
                userName = response.displayName,
                email = response.email,
                phoneNumber = response.phoneNumber,
                avatar = response.avatar
            )
        } catch (e: Exception) {
            printlnCK("getProfileWithGrpc: $e")
            return@withContext null
        }
    }

    private suspend fun peerRegisterClientKeyWithGrpc(owner: Owner, signalGrpc: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("peerRegisterClientKeyWithGrpc, clientId = ${owner.clientId}, domain = ${owner.domain}")
        try {
            val address = CKSignalProtocolAddress(owner, 111)

            val identityKeyPair = myStore.identityKeyPair

            val preKey = signalKeyRepository.getPreKey()
            val signedPreKey = signalKeyRepository.getSignedKey()

            val request = Signal.PeerRegisterClientKeyRequest.newBuilder()
                .setClientId(address.owner.clientId)
                .setDeviceId(address.deviceId)
                .setRegistrationId(myStore.localRegistrationId)
                .setIdentityKeyPublic(ByteString.copyFrom(identityKeyPair.publicKey.serialize()))
                .setPreKey(ByteString.copyFrom(preKey.serialize()))
                .setPreKeyId(preKey.id)
                .setSignedPreKeyId(signedPreKey.id)
                .setSignedPreKey(
                    ByteString.copyFrom(signedPreKey.serialize())
                )
                .setSignedPreKeySignature(ByteString.copyFrom(signedPreKey.signature))
                .build()

            val response = signalGrpc.peerRegisterClientKey(request)
            if (response?.success != false) {
                printlnCK("peerRegisterClientKeyWithGrpc, success")
                return@withContext true
            }
        } catch (e: Exception) {
            printlnCK("peerRegisterClientKeyWithGrpc: $e")
        }

        return@withContext false
    }
}