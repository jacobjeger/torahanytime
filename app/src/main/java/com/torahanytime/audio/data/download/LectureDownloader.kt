package com.torahanytime.audio.data.download

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.torahanytime.audio.TATApplication
import com.torahanytime.audio.data.local.entity.DownloadedLecture
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.ui.common.SnackbarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Singleton that manages downloading lecture MP3 files to local storage.
 * Features: progress tracking, exponential backoff retry, disk space checks,
 * file validation, and auto-cleanup of completed downloads.
 */
object LectureDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val downloadDao by lazy { TATApplication.db.downloadDao() }
    private val favoriteDao by lazy { TATApplication.db.favoriteDao() }
    private val historyDao by lazy { TATApplication.db.historyDao() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Map of lectureId -> download progress (0f..1f), -1f = error
    private val _downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Float>> = _downloadProgress

    // Currently downloading lecture IDs
    private val activeDownloads = mutableSetOf<Int>()

    // Retry tracking: lectureId -> retry count
    private val retryCounts = mutableMapOf<Int, Int>()
    private val retryData = mutableMapOf<Int, Pair<Context, Lecture>>() // stored for retry

    // Config
    private const val MAX_RETRIES = 3
    private const val RETRY_BASE_DELAY_MS = 10_000L // 10s base, exponential: 10s, 30s, 90s
    private const val MIN_FREE_SPACE_BYTES = 100L * 1024 * 1024 // 100MB
    private const val MIN_VALID_FILE_SIZE = 10_000L // 10KB minimum for valid MP3
    private const val AUTO_DELETE_DELAY_MS = 48L * 60 * 60 * 1000 // 48 hours

    fun isDownloading(lectureId: Int): Boolean = activeDownloads.contains(lectureId)

    /**
     * Downloads a lecture MP3 file to local storage.
     * On failure, automatically retries with exponential backoff (up to MAX_RETRIES).
     * Returns true if successful, false otherwise.
     */
    suspend fun download(context: Context, lecture: Lecture): Boolean {
        val mp3Url = lecture.mp3Url ?: return false
        if (lecture.noDownload == true) return false
        if (activeDownloads.contains(lecture.id)) return false

        // Check if already downloaded
        val existing = downloadDao.getByLectureId(lecture.id)
        if (existing != null && File(existing.localFilePath).exists()) return true

        // Check disk space
        if (!hasEnoughDiskSpace(context)) {
            updateProgress(lecture.id, -1f)
            return false
        }

        activeDownloads.add(lecture.id)
        retryData[lecture.id] = context to lecture
        updateProgress(lecture.id, 0f)
        SnackbarManager.show("Download started")

        return withContext(Dispatchers.IO) {
            try {
                val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    ?: context.filesDir
                val tatDir = File(musicDir, "tat")
                if (!tatDir.exists()) tatDir.mkdirs()

                val outputFile = File(tatDir, "${lecture.id}.mp3")
                val request = Request.Builder()
                    .url(mp3Url)
                    .header("User-Agent", "TorahAnytimeAudio/2.1")
                    .build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    handleFailure(lecture.id, "HTTP ${response.code}")
                    return@withContext false
                }

                val body = response.body ?: run {
                    handleFailure(lecture.id, "Empty response body")
                    return@withContext false
                }

                val contentLength = body.contentLength()
                var bytesWritten = 0L

                FileOutputStream(outputFile).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesWritten += read
                            if (contentLength > 0) {
                                updateProgress(lecture.id, bytesWritten.toFloat() / contentLength)
                            }
                        }
                    }
                }

                // Validate downloaded file
                if (outputFile.length() < MIN_VALID_FILE_SIZE) {
                    outputFile.delete()
                    handleFailure(lecture.id, "File too small (${outputFile.length()} bytes)")
                    return@withContext false
                }

                // Save to Room DB
                downloadDao.upsert(
                    DownloadedLecture(
                        lectureId = lecture.id,
                        title = lecture.title,
                        speakerName = lecture.speakerFullName,
                        thumbnailUrl = lecture.thumbnailUrl,
                        mp3Url = mp3Url,
                        localFilePath = outputFile.absolutePath,
                        duration = lecture.duration ?: 0,
                        languageName = lecture.languageName,
                        fileSizeBytes = outputFile.length()
                    )
                )

                // Success — clear retry state
                updateProgress(lecture.id, 1f)
                activeDownloads.remove(lecture.id)
                retryCounts.remove(lecture.id)
                retryData.remove(lecture.id)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                // Clean up partial file
                try {
                    val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        ?: context.filesDir
                    File(musicDir, "tat/${lecture.id}.mp3").delete()
                } catch (_: Exception) {}

                handleFailure(lecture.id, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * Handle download failure — schedule retry with exponential backoff if under limit.
     */
    private fun handleFailure(lectureId: Int, reason: String) {
        activeDownloads.remove(lectureId)
        val currentRetry = retryCounts.getOrDefault(lectureId, 0)

        if (currentRetry < MAX_RETRIES && retryData.containsKey(lectureId)) {
            retryCounts[lectureId] = currentRetry + 1
            val delayMs = RETRY_BASE_DELAY_MS * pow3(currentRetry) // 10s, 30s, 90s
            updateProgress(lectureId, -2f) // -2f = retrying

            scope.launch {
                delay(delayMs)
                val (ctx, lecture) = retryData[lectureId] ?: return@launch
                download(ctx, lecture)
            }
        } else {
            // Max retries exhausted
            updateProgress(lectureId, -1f)
            retryCounts.remove(lectureId)
            retryData.remove(lectureId)
        }
    }

    /** Manual retry for a failed download */
    fun retryFailed(context: Context, lecture: Lecture) {
        retryCounts.remove(lecture.id)
        scope.launch {
            download(context, lecture)
        }
    }

    private fun pow3(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 3 }
        return result
    }

    /**
     * Check if there's enough free disk space for a download.
     */
    private fun hasEnoughDiskSpace(context: Context): Boolean {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir
            val stat = StatFs(dir.path)
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            freeBytes > MIN_FREE_SPACE_BYTES
        } catch (_: Exception) {
            true // If we can't check, allow the download
        }
    }

    /**
     * Returns the local file path if the lecture is downloaded, null otherwise.
     */
    suspend fun getLocalPath(lectureId: Int): String? {
        val dl = downloadDao.getByLectureId(lectureId) ?: return null
        val file = File(dl.localFilePath)
        return if (file.exists()) dl.localFilePath else {
            // File missing — clean up DB record
            downloadDao.delete(lectureId)
            null
        }
    }

    /**
     * Auto-delete downloads that have been fully listened (>95% complete)
     * and were downloaded more than 48 hours ago.
     * Protects favorites from auto-deletion.
     */
    suspend fun cleanupCompletedDownloads() {
        withContext(Dispatchers.IO) {
            try {
                val allDownloads = downloadDao.getAllSync()
                val now = System.currentTimeMillis()
                var deletedCount = 0

                for (dl in allDownloads) {
                    // Skip if downloaded less than 48h ago
                    if (now - dl.downloadedAt < AUTO_DELETE_DELAY_MS) continue

                    // Skip if favorited
                    if (favoriteDao.isFavoriteSync(dl.lectureId)) continue

                    // Check if fully listened (>95% of duration)
                    val history = historyDao.getByLectureId(dl.lectureId) ?: continue
                    val durationMs = dl.duration * 1000L
                    if (durationMs > 0 && history.position >= durationMs * 0.95) {
                        // Delete the file and DB record
                        File(dl.localFilePath).delete()
                        downloadDao.delete(dl.lectureId)
                        deletedCount++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Get total storage used by downloads in bytes.
     */
    suspend fun getTotalStorageUsed(): Long {
        return downloadDao.getTotalSize() ?: 0L
    }

    /**
     * Get free disk space in bytes.
     */
    fun getFreeDiskSpace(context: Context): Long {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir
            val stat = StatFs(dir.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            0L
        }
    }

    private fun updateProgress(lectureId: Int, progress: Float) {
        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
            put(lectureId, progress)
        }
    }

    /**
     * Clear progress entry for a lecture (call after UI has consumed the state).
     */
    fun clearProgress(lectureId: Int) {
        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
            remove(lectureId)
        }
    }
}
