package com.example.sbctracker.work

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sbctracker.db.SbcTrackerDatabase.Companion.getDatabase
import com.example.sbctracker.repository.LastLocationRepository

class CacheUploadWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val database = getDatabase(applicationContext)
        val repository = LastLocationRepository(database.lastLocationDao())
        return Result.success()
    }

}