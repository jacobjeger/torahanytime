package com.torahanytime.audio

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.torahanytime.audio.data.api.AuthManager
import com.torahanytime.audio.data.download.LectureDownloader
import com.torahanytime.audio.data.local.SpeakerSpeedStore
import com.torahanytime.audio.data.local.TATDatabase
import com.torahanytime.audio.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TATApplication : Application() {

    lateinit var database: TATDatabase
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        AuthManager.init(this)
        database = TATDatabase.getInstance(this)
        NetworkMonitor.init(this)
        SpeakerSpeedStore.init(this)

        // Configure Coil image loading with caching
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.20) // 20% of app memory
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(50L * 1024 * 1024) // 50MB
                        .build()
                }
                .crossfade(300)
                .build()
        )

        // Auto-cleanup completed downloads (48h after fully listened)
        appScope.launch {
            LectureDownloader.cleanupCompletedDownloads()
        }
    }

    companion object {
        lateinit var instance: TATApplication
            private set

        val db: TATDatabase get() = instance.database
    }
}
