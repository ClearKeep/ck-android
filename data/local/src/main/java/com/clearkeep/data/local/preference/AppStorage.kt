package com.clearkeep.data.local.preference

import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.local.preference.PersistPreferencesStorage
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val DEVICE_ID = "device_id"

@Singleton
class AppStorage @Inject constructor(
    private val persistStorage: PersistPreferencesStorage,
) {
    fun getUniqueDeviceID(): String {
        var deviceId = persistStorage.getString(DEVICE_ID)
        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            printlnCK("generate new device id: $deviceId")
            persistStorage.setString(DEVICE_ID, deviceId)
        }
        return deviceId
    }
}