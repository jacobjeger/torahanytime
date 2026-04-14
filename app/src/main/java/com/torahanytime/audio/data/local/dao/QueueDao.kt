package com.torahanytime.audio.data.local.dao

import androidx.room.*
import com.torahanytime.audio.data.local.entity.QueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun getAll(): Flow<List<QueueItem>>

    @Query("SELECT COUNT(*) FROM queue_items")
    fun getCount(): Flow<Int>

    @Upsert
    suspend fun upsert(item: QueueItem)

    @Insert
    suspend fun insert(item: QueueItem)

    @Query("DELETE FROM queue_items WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM queue_items")
    suspend fun deleteAll()

    @Query("SELECT MAX(position) FROM queue_items")
    suspend fun getMaxPosition(): Int?

    @Query("SELECT * FROM queue_items ORDER BY position ASC LIMIT 1")
    suspend fun getNext(): QueueItem?

    @Query("DELETE FROM queue_items WHERE id = (SELECT id FROM queue_items ORDER BY position ASC LIMIT 1)")
    suspend fun removeFirst()
}
