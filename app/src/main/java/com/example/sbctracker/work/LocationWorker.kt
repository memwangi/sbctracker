package com.example.sbctracker.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.sbctracker.db.SbcTrackerDatabase
import com.example.sbctracker.repository.LastLocationRepository
import com.google.android.gms.location.LocationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private val TAG = "Location Worker"
    private val locationDao = SbcTrackerDatabase.getDatabase(applicationContext).lastLocationDao()
    private val repository = LastLocationRepository(locationDao, context)


    override fun doWork(): Result {
        var identifier = inputData.getString("identifier")
        try {
            if (identifier != null) {
                sendLocation(identifier)
            }
        } catch (e: Exception) {
            Log.d("$TAG", "Exception getting location -->  ${e.message}")
            return Result.retry()
        }
        return Result.success()
    }

    private fun sendLocation(identifier: String) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                repository.addLocation(identifier)
            }
        }
    }
}