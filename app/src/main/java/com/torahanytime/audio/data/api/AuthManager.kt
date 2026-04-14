package com.torahanytime.audio.data.api

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AuthManager {
    private const val PREFS_NAME = "tat_auth"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_EXPIRATION = "expiration"

    private lateinit var prefs: SharedPreferences

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        val expiration = prefs.getLong(KEY_EXPIRATION, 0)
        _isLoggedIn.value = token != null && expiration > System.currentTimeMillis()
        _userEmail.value = prefs.getString(KEY_EMAIL, null)
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
    }
}
