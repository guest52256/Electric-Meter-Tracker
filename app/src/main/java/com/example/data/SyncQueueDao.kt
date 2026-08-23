package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE syncStatus != 'COMPLETED' ORDER BY createdAt ASC")
    fun getPendingOperationsFlow(): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE syncStatus != 'COMPLETED' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncQueueItem>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncStatus != 'COMPLETED'")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(item: SyncQueueItem)

    @Update
    suspend fun updateOperation(item: SyncQueueItem)

    @Query("DELETE FROM sync_queue WHERE documentId = :idOrDocId OR operationId = :idOrDocId")
    suspend fun deleteOperation(idOrDocId: String)

    @Query("DELETE FROM sync_queue WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: String)

    @Query("DELETE FROM sync_queue WHERE syncStatus = 'COMPLETED'")
    suspend fun clearCompletedOperations()

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
