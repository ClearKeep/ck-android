package com.clearkeep.utilities

import java.math.BigInteger
import javax.crypto.spec.IvParameterSpec

import java.security.AlgorithmParameters
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

import javax.crypto.spec.SecretKeySpec

import javax.crypto.SecretKey

import javax.crypto.spec.PBEKeySpec

import java.security.spec.KeySpec
import javax.crypto.Cipher

import javax.crypto.SecretKeyFactory

class DecryptsPBKDF2 @Throws(Exception::class) constructor(private val passPhrase: String) {
    private var dcipher: Cipher? = null
    var iterationCount = 1024
    var keyStrength = 256
    var factory: SecretKeyFactory
    private var key: SecretKey? = null
    private lateinit var iv: ByteArray
    private lateinit var saltEncrypt: ByteArray

    init {
        dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    }

    @Throws(Exception::class)
    fun encrypt(data: ByteArray, saltHex: String): ByteArray? {
        saltEncrypt = fromHex(saltHex)
        val spec: KeySpec =
            PBEKeySpec(passPhrase.toCharArray(), saltEncrypt, iterationCount, keyStrength)
        val tmp = factory.generateSecret(spec)
        key = SecretKeySpec(tmp.encoded, "AES")
        dcipher?.init(Cipher.ENCRYPT_MODE, key)
        val params: AlgorithmParameters? = dcipher?.parameters
        iv = params?.getParameterSpec(IvParameterSpec::class.java)?.iv!!
        return dcipher?.doFinal(data)
    }

    @kotlin.jvm.Throws(Exception::class)
    fun encrypt(data: ByteArray, salt: String, oldIv: String): ByteArray? {
        saltEncrypt = fromHex(salt)
        val spec: KeySpec =
            PBEKeySpec(passPhrase.toCharArray(), saltEncrypt, iterationCount, keyStrength)
        val tmp = factory.generateSecret(spec)
        key = SecretKeySpec(tmp.encoded, "AES")
        dcipher?.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(fromHex(oldIv)))
        val params: AlgorithmParameters? = dcipher?.parameters
        iv = params?.getParameterSpec(IvParameterSpec::class.java)?.iv!!
        return dcipher?.doFinal(data)
    }

    @Throws(Exception::class)
    fun decrypt(
        base64EncryptedData: ByteArray,
        salt: ByteArray,
        ivParameterSpec: ByteArray
    ): ByteArray? {
        val spec: KeySpec = PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount, keyStrength)
        printlnCK("decrypt spec success")
        val tmp = factory.generateSecret(spec)
        printlnCK("decrypt gen secret success")
        key = SecretKeySpec(tmp.encoded, "AES")
        dcipher?.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ivParameterSpec))
        printlnCK("decrypt dcipher init success")
        return dcipher?.doFinal(base64EncryptedData) ?: return null
    }

    fun getIv(): ByteArray {
        return iv
    }

    companion object {
        @Throws(NoSuchAlgorithmException::class)
        fun toHex(array: ByteArray): String {
            val bi = BigInteger(1, array)
            val hex = bi.toString(16)
            val paddingLength = array.size * 2 - hex.length
            return if (paddingLength > 0) {
                String.format("%0" + paddingLength + "d", 0) + hex
            } else {
                hex
            }
        }

        @Throws(NoSuchAlgorithmException::class)
        fun fromHex(hex: String): ByteArray {
            val bytes = ByteArray(hex.length / 2)
            for (i in bytes.indices) {
                bytes[i] = hex.substring(2 * i, 2 * i + 2).toInt(16).toByte()
            }
            return bytes
        }
    }
}