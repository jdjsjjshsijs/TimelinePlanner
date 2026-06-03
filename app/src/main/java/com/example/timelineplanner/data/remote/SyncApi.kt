package com.example.timelineplanner.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

interface SyncApi {

    @POST("api/tasks/sync")
    suspend fun syncTasks(@Body request: SyncRequest): SyncResponse

    @GET("api/tasks")
    suspend fun getTasksByDate(@Query("date") dateMillis: Long): TasksResponse

    @GET("api/tasks/all")
    suspend fun getAllTasks(): TasksResponse

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") taskId: Long)
}
