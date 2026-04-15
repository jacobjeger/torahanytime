package com.torahanytime.audio.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

object AuthManager {
    private const val PREFS_NAME = "tat_auth_encrypted"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_EXPIRATION = "expiration"

    private lateinit var prefs: SharedPreferences

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail

    private val _logoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvents: SharedFlow<Unit> = _logoutEvents

    fun init(context: Context) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Migrate from old unencrypted prefs if they exist
        migrateFromPlainPrefs(context)

        val token = prefs.getString(KEY_TOKEN, null)
        val expiration = prefs.getLong(KEY_EXPIRATION, 0)
        _isLoggedIn.value = token != null && expiration > System.currentTimeMillis()
        _userEmail.value = prefs.getString(KEY_EMAIL, null)
    }

    private fun migrateFromPlainPrefs(context: Context) {
        val oldPrefs = context.getSharedPreferences("tat_auth", Context.MODE_PRIVATE)
        val oldToken = oldPrefs.getString(KEY_TOKEN, null)
        if (oldToken != null) {
            // Copy data to encrypted prefs
            prefs.edit()
                .putString(KEY_TOKEN, oldToken)
                .putString(KEY_USER_ID, oldPrefs.getString(KEY_USER_ID, null))
                .putString(KEY_EMAIL, oldPrefs.getString(KEY_EMAIL, null))
                .putLong(KEY_EXPIRATION, oldPrefs.getLong(KEY_EXPIRATION, 0))
                .apply()
            // Clear old unencrypted prefs
            oldPrefs.edit().clear().apply()
        }
    }

    fun saveLogin(token: String, userId: String, email: String, expiration: Long) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putLong(KEY_EXPIRATION, expiration)
            .apply()
        _isLoggedIn.value = true
        _userEmail.value = email
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        val expiration = prefs.getLong(KEY_EXPIRATION, 0)
        if (token != null && expiration > System.currentTimeMillis()) {
            return token
        }
        return null
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun logout() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        _userEmail.value = null
        _logoutEvents.tryEmit(Unit)
    }
}
