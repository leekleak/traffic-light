package com.leekleak.trafficlight.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

val Context.cryptoStore: DataStore<Preferences> by preferencesDataStore(name = "crypt_store")

object CryptoManager {
    private const val KEY_ALIAS = "data_plan_db_password"
    private val ENCRYPTED_KEY = stringPreferencesKey("encrypted_data_plan_key")

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false)
                        .build()
                )
            }.generateKey()
    }

    private fun encryptPassphrase(passphrase: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        }
        val blob = cipher.iv + cipher.doFinal(passphrase)
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    private fun decryptPassphrase(encoded: String): ByteArray {
        val blob = Base64.decode(encoded, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, blob, 0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), spec)
        }
        return cipher.doFinal(blob, 12, blob.size - 12)
    }

    suspend fun getOrCreateDbPassphrase(context: Context): ByteArray {
        val existing = context.cryptoStore.data
            .map { it[ENCRYPTED_KEY] }
            .first()

        if (existing != null) {
            return decryptPassphrase(existing)
        }
        val passphrase = generateDbPassphrase()
        val encrypted = encryptPassphrase(passphrase)

        context.cryptoStore.edit { it[ENCRYPTED_KEY] = encrypted }

        return passphrase
    }

    private fun generateDbPassphrase(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
}