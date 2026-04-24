package com.leekleak.trafficlight.database

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.leekleak.trafficlight.model.NetworkUsageManager.Companion.NULL_SUBSCRIBER
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALIAS = "data_plan_key"
    private const val HMAC_ALIAS = "hmac_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getSecretKey(alias: String): SecretKey {
        val existingKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey(alias)
    }

    private fun createKey(alias: String): SecretKey {
        val isHmac = alias == HMAC_ALIAS
        val algorithm = if (isHmac) KeyProperties.KEY_ALGORITHM_HMAC_SHA256 else KeyProperties.KEY_ALGORITHM_AES
        val purpose = if (isHmac) KeyProperties.PURPOSE_SIGN else (KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)

        val keyGenerator = KeyGenerator.getInstance(algorithm, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(alias, purpose)

        if (!isHmac) {
            builder.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        }

        keyGenerator.init( builder.build() )
        return keyGenerator.generateKey()
    }

    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(KEY_ALIAS))
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encryptedData: String): String = runCatching {
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
        val iv = combined.sliceArray(0 until 12)
        val ciphertext = combined.sliceArray(12 until combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(KEY_ALIAS), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull() ?: NULL_SUBSCRIBER

    fun hashIdentifier(id: String): String {
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(getSecretKey(HMAC_ALIAS))
        val result = hmac.doFinal(id.toByteArray(Charsets.UTF_8))
        return result.joinToString("") { "%02x".format(it) }
    }
}