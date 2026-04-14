package com.torahanytime.audio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_lectures")
data class DownloadedLecture(
    @PrimaryKey val lectureId: Int,
    val title: String,
    val speakerName: String,
    val thumbnailUrl: String? = null,
    val mp3Url: String? = null,
    val localFilePath: String,
    val duration: Int = 0,
    val languageName: String? = null,
    val fileSizeBytes: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)
