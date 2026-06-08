package com.example.timelineplanner.data.repository

import com.example.timelineplanner.data.db.PracticeDao
import com.example.timelineplanner.data.db.PracticeRecordEntity
import com.example.timelineplanner.data.db.PracticeSubjectEntity
import com.example.timelineplanner.model.PracticeRecord
import com.example.timelineplanner.model.PracticeSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeRepository @Inject constructor(
    private val practiceDao: PracticeDao
) {
    fun getAllSubjects(): Flow<List<PracticeSubject>> {
        return practiceDao.getAllSubjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAllSubjectsOnce(): List<PracticeSubject> {
        return practiceDao.getAllSubjectsOnce().map { it.toDomain() }
    }

    suspend fun getSubjectById(id: Long): PracticeSubject? {
        return practiceDao.getSubjectById(id)?.toDomain()
    }

    suspend fun insertSubject(subject: PracticeSubject): Long {
        return practiceDao.insertSubject(subject.toEntity())
    }

    suspend fun updateSubject(subject: PracticeSubject) {
        practiceDao.updateSubject(subject.toEntity())
    }

    suspend fun deleteSubjectById(id: Long) {
        practiceDao.deleteSubjectById(id)
    }

    fun getRecordsBySubject(subjectId: Long): Flow<List<PracticeRecord>> {
        return practiceDao.getRecordsBySubject(subjectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getRecordsBySubjectOnce(subjectId: Long): List<PracticeRecord> {
        return practiceDao.getRecordsBySubjectOnce(subjectId).map { it.toDomain() }
    }

    suspend fun getAllRecordsOnce(): List<PracticeRecord> {
        return practiceDao.getAllRecordsOnce().map { it.toDomain() }
    }

    suspend fun insertRecord(record: PracticeRecord): Long {
        return practiceDao.insertRecord(record.toEntity())
    }

    suspend fun deleteRecordById(id: Long) {
        practiceDao.deleteRecordById(id)
    }

    // For server restore
    suspend fun insertSubjectWithId(subject: PracticeSubject): Long {
        val existing = practiceDao.getSubjectById(subject.id)
        if (existing != null) {
            practiceDao.updateSubject(subject.toEntity())
            return subject.id
        }
        return practiceDao.insertSubject(subject.toEntity())
    }

    suspend fun insertRecordWithId(record: PracticeRecord): Long {
        return practiceDao.insertRecord(record.toEntity())
    }

    private fun PracticeSubjectEntity.toDomain() = PracticeSubject(
        id = id, name = name, color = color, createdAt = createdAt
    )

    private fun PracticeSubject.toEntity() = PracticeSubjectEntity(
        id = id, name = name, color = color, createdAt = createdAt
    )

    private fun PracticeRecordEntity.toDomain() = PracticeRecord(
        id = id, subjectId = subjectId, totalQuestions = totalQuestions,
        correctQuestions = correctQuestions, accuracy = accuracy,
        dateMillis = dateMillis, notes = notes, createdAtMillis = createdAtMillis
    )

    private fun PracticeRecord.toEntity() = PracticeRecordEntity(
        id = id, subjectId = subjectId, totalQuestions = totalQuestions,
        correctQuestions = correctQuestions, accuracy = accuracy,
        dateMillis = dateMillis, notes = notes, createdAtMillis = createdAtMillis
    )
}
