package com.thetheobot.shelfplayer

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

private const val CONNECTION_PREFS_FILE = "connection_credentials"
private const val KEY_SERVER_URL = "server_url"
private const val KEY_AUTH_DATA = "auth_data"

data class ConnectionCredentials(
    val serverUrl: String,
    val accessToken: String,
)

interface ConnectionCredentialsStore {
    fun load(): ConnectionCredentials?
    fun save(credentials: ConnectionCredentials): Boolean
}

class EncryptedConnectionCredentialsStore(
    private val sharedPreferences: SharedPreferences,
) : ConnectionCredentialsStore {
    override fun load(): ConnectionCredentials? {
        val serverUrl = sharedPreferences.getString(KEY_SERVER_URL, null)?.trim().orEmpty()
        val accessToken = sharedPreferences.getString(KEY_AUTH_DATA, null)?.trim().orEmpty()

        return if (serverUrl.isNotBlank() && accessToken.isNotBlank()) {
            ConnectionCredentials(
                serverUrl = serverUrl,
                accessToken = accessToken,
            )
        } else {
            null
        }
    }

    override fun save(credentials: ConnectionCredentials): Boolean {
        return sharedPreferences.edit()
            .putString(KEY_SERVER_URL, credentials.serverUrl)
            .putString(KEY_AUTH_DATA, credentials.accessToken.trim())
            .commit()
    }

    companion object {
        fun create(context: Context): ConnectionCredentialsStore {
            val appContext = context.applicationContext
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            val sharedPreferences = EncryptedSharedPreferences.create(
                appContext,
                CONNECTION_PREFS_FILE,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

            return EncryptedConnectionCredentialsStore(sharedPreferences)
        }
    }
}
