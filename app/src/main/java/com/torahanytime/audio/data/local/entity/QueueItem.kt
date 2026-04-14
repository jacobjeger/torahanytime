package com.torahanytime.audio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue_items")
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lectureId: Int,
    val title: String,
    val speakerName: String,
    val thumbnailUrl: String? = null,
    val mp3Url: String? = null,
    val duration: Int = 0,
    val languageName: String? = null,
    val position: Int = 0
)
