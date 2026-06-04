package com.example.timelineplanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {

    // Subject operations
    @Query("SELECT * FROM practice_subjects ORDER BY createdAt DESC")
    fun getAllSubjects(): Flow<List<PracticeSubjectEntity>>

    @Query("SELECT * FROM practice_subjects ORDER BY createdAt DESC")
    suspend fun getAllSubjectsOnce(): List<PracticeSubjectEntity>

    @Query("SELECT * FROM practice_subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): PracticeSubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: PracticeSubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: PracticeSubjectEntity)

    @Query("DELETE FROM practice_subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)

    // Record operations
    @Query("SELECT * FROM practice_records WHERE subjectId = :subjectId ORDER BY dateMillis DESC")
    fun getRecordsBySubject(subjectId: Long): Flow<List<PracticeRecordEntity>>

    @Query("SELECT * FROM practice_records WHERE subjectId = :subjectId ORDER BY dateMillis DESC")
    suspend fun getRecordsBySubjectOnce(subjectId: Long): List<PracticeRecordEntity>

    @Query("SELECT * FROM practice_records WHERE dateMillis >= :startMillis AND dateMillis < :endMillis")
    suspend fun getRecordsByDateRange(startMillis: Long, endMillis: Long): List<PracticeRecordEntity>

    @Query("SELECT * FROM practice_records")
    suspend fun getAllRecordsOnce(): List<PracticeRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PracticeRecordEntity): Long

    @Query("DELETE FROM practice_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM practice_records WHERE subjectId = :subjectId")
    suspend fun deleteRecordsBySubjectId(subjectId: Long)
}
