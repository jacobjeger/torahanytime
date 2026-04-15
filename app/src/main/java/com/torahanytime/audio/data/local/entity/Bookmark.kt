package com.torahanytime.audio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lectureId: Int,
    val position: Long,
    val note: String = "",
    val lectureTitle: String = "",
    val speakerName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
