package com.torahanytime.audio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listening_history")
data class ListeningHistory(
    @PrimaryKey val lectureId: Int,
    val title: String,
    val speakerName: String,
    val thumbnailUrl: String? = null,
    val mp3Url: String? = null,
    val duration: Int = 0,
    val position: Long = 0,
    val languageName: String? = null,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
