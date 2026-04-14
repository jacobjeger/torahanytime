package com.torahanytime.audio

import android.app.Application
import com.torahanytime.audio.data.api.AuthManager
import com.torahanytime.audio.data.local.TATDatabase

class TATApplication : Application() {

    lateinit var database: TATDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        AuthManager.init(this)
        database = TATDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: TATApplication
            private set

        val db: TATDatabase get() = instance.database
    }
}
