package com.example.timelineplanner.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

data class SyncTask(
    val id: Long,
    val title: String,
    val dateMillis: Long,
    val startMinute: Int,
    val endMinute: Int,
    val color: String,
    val notes: String,
    val orderIndex: Int,
    val pauseSegments: List<List<Int>>
)

data class SyncRequest(
    val dateMillis: Long,
    val tasks: List<SyncTask>
)

data class SyncResponse(
    val ok: Boolean,
    val count: Int
)

data class TasksResponse(
    val tasks: List<SyncTask>
)

// Practice sync models
data class SyncPracticeSubject(
    val id: Long,
    val name: String,
    val color: String,
    val createdAt: Long
)

data class SyncPracticeRecord(
    val id: Long,
    val subjectId: Long,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val accuracy: Float,
    val dateMillis: Long,
    val notes: String,
    val createdAtMillis: Long = 0
)

data class SyncPracticeRequest(
    val subjects: List<SyncPracticeSubject>,
    val records: List<SyncPracticeRecord>
)

data class PracticeResponse(
    val subjects: List<SyncPracticeSubject>,
    val records: List<SyncPracticeRecord>
)

// Course sync models
data class SyncCourse(
    val id: Long,
    val title: String,
    val location: String,
    val teacher: String,
    val daysOfWeek: String,
    val startMinute: Int,
    val endMinute: Int,
    val color: String,
    val notes: String,
    val startDate: Long,
    val endDate: Long
)

data class CoursesResponse(
    val courses: List<SyncCourse>
)

// Goal sync models
data class SyncGoal(
    val id: Long,
    val name: String,
    val deadlineMillis: Long,
    val color: String,
    val createdAt: Long
)

data class GoalsResponse(
    val goals: List<SyncGoal>
)

interface SyncApi {

    @POST("api/tasks/sync")
    suspend fun syncTasks(@Body request: SyncRequest): SyncResponse

    @GET("api/tasks")
    suspend fun getTasksByDate(@Query("date") dateMillis: Long): TasksResponse

    @GET("api/tasks/all")
    suspend fun getAllTasks(): TasksResponse

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") taskId: Long)

    // Practice endpoints
    @POST("api/practice/sync")
    suspend fun syncPractice(@Body request: SyncPracticeRequest): SyncResponse

    @GET("api/practice/all")
    suspend fun getAllPractice(): PracticeResponse

    @DELETE("api/practice/records/{id}")
    suspend fun deletePracticeRecord(@Path("id") recordId: Long): SyncResponse

    @DELETE("api/practice/subjects/{id}")
    suspend fun deletePracticeSubject(@Path("id") subjectId: Long): SyncResponse

    // Course endpoints
    @POST("api/courses/sync")
    suspend fun syncCourses(@Body courses: List<SyncCourse>): SyncResponse

    @GET("api/courses/all")
    suspend fun getAllCourses(): CoursesResponse

    @DELETE("api/courses/{id}")
    suspend fun deleteCourse(@Path("id") courseId: Long): SyncResponse

    // Goal endpoints
    @POST("api/goals/sync")
    suspend fun syncGoals(@Body goals: List<SyncGoal>): SyncResponse

    @GET("api/goals/all")
    suspend fun getAllGoals(): GoalsResponse

    @DELETE("api/goals/{id}")
    suspend fun deleteGoal(@Path("id") goalId: Long): SyncResponse

    // Ping endpoint for latency monitoring
    @GET("api/ping")
    suspend fun ping(): Response<Unit>
}

