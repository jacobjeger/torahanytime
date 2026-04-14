package com.torahanytime.audio

import android.app.Application
import com.torahanytime.audio.data.api.AuthManager

class TATApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
    }
}
