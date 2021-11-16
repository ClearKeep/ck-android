package com.clearkeep.data.remote

import com.clearkeep.db.clearkeep.model.Server
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import notify_push.NotifyPushOuterClass
import javax.inject.Inject

class PushNotificationService @Inject constructor(
    private val apiProvider: ParamAPIProvider,
) {
    suspend fun registerPushNotificationToken(deviceId: String, token: String, server: Server): NotifyPushOuterClass.BaseResponse = withContext(Dispatchers.IO) {
        val request = NotifyPushOuterClass.RegisterTokenRequest.newBuilder()
            .setDeviceId(deviceId)
            .setDeviceType("android")
            .setToken(token)
            .build()
        val notifyPushGrpc = apiProvider.provideNotifyPushBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )
        return@withContext notifyPushGrpc.registerToken(request)
    }
}