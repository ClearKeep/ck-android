package com.clearkeep.secure

import android.text.TextUtils
import com.clearkeep.data.DataStore
import com.clearkeep.db.UserRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.protobuf.ByteString

object CryptoHelper {
    val keys = mutableMapOf<String, KeySet>()

    data class KeySend(
        var signing: ByteArray?,
        var agreement: ByteArray
    )

    data class KeySet(
        var ourSigning: ByteArray,
        var ourAgreement: ByteArray,
        var theirSigning: ByteArray?,
        var theirAgreement: ByteArray?
    )

    fun initKeysSession(keyPair: String?) {
        if (!TextUtils.isEmpty(keyPair)) {
            val type = object : TypeToken<MutableMap<String, KeySet>>() {}.getType()
            val listDeviceConnect: MutableMap<String, KeySet> =
                Gson().fromJson(keyPair, type)

            for (item in listDeviceConnect) {
                keys.put(item.key, item.value)
            }
        }
    }

    fun getKeySet(id: String): KeySet? {
        return keys.get(id)
    }


    fun checkHandShaked(id: String): Boolean {
        val keyset = getKeySet(id)
        if (keyset != null && keyset.theirAgreement != null) {
            return true
        }
        return false
    }


    // returns false if this is a response to the handshake we sent
    fun set(agreement: ByteArray, sender: String, dbLocal: UserRepository): Boolean {
        val key = getKeySet(sender)
        if (key != null) {
            keys[sender]!!.theirAgreement = agreement

            val currentUser = dbLocal.getUserByName(DataStore.username)
            if (!TextUtils.isEmpty(currentUser?.security)) {
                val type = object : TypeToken<MutableMap<String, KeySet>>() {}.getType()
                val listDeviceConnect: MutableMap<String, KeySet> =
                    Gson().fromJson(currentUser!!.security, type)
                listDeviceConnect.set(sender, getKeySet(sender)!!)

                currentUser.security = Gson().toJson(listDeviceConnect)
                dbLocal.updateUser(currentUser)
            } else {

                val listDeviceConnect = mutableMapOf<String, KeySet>()
                listDeviceConnect.set(sender, getKeySet(sender)!!)

                currentUser!!.security = Gson().toJson(listDeviceConnect)
                dbLocal.updateUser(currentUser)
            }


            return false
        } else {
//            val privateKey = Curve25519Donna().getPrivateKey()
//            val publishKey = Curve25519Donna().getPublicKey(privateKey)
            keys[sender] = KeySet(
                agreement,
                agreement,
                null,
                agreement
            )

            val currentUser = dbLocal.getUserByName(DataStore.username)
            if (!TextUtils.isEmpty(currentUser?.security)) {
                val type = object : TypeToken<MutableMap<String, KeySet>>() {}.getType()
                val listDeviceConnect: MutableMap<String, KeySet> =
                    Gson().fromJson(currentUser!!.security, type)
                listDeviceConnect.set(sender, keys[sender]!!)

                currentUser.security = Gson().toJson(listDeviceConnect)
                dbLocal.updateUser(currentUser)

            } else {
                val listDeviceConnect = mutableMapOf<String, KeySet>()
                listDeviceConnect.set(sender, getKeySet(sender)!!)

                currentUser!!.security = Gson().toJson(listDeviceConnect)
                dbLocal.updateUser(currentUser)
            }
        }
        return true
    }

    fun getKeySendTo(recipient: String): KeySend {
        val key = keys[recipient]
        if (null != key) {
            return KeySend(key.ourSigning, key.ourAgreement)
        } else {
            // Generate my KeyPair
//            val privateKey =Curve25519Donna().getPrivateKey()
//            val publishKey = Curve25519Donna().getPublicKey(privateKey)
//            keys[recipient] = KeySet(privateKey, publishKey, null, null)
//            return KeySend(privateKey, publishKey)
            return KeySend(key?.ourSigning, key!!.ourAgreement)
        }
    }

    fun getSecretKey(keySet: KeySet): ByteArray? {
//        return Curve25519Donna().getSharedKey(keySet.ourSigning, keySet.theirAgreement!!)
        return null
    }
}
