package com.example.sbctracker

import androidx.multidex.MultiDexApplication
import androidx.work.*
import com.example.sbctracker.work.LocationTrackingWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class TraceApplication : MultiDexApplication() {


    /**
     * onCreate is called before the first screen is shown to the user.
     * Use it to setup any background tasks, running expensive setup operations in a background
     * thread to avoid delaying app start.
     */
    private val applicationScope = CoroutineScope(Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
//        delayedInit()
    }

//    private fun setUpRecurringWork() {
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
//
//        val repeatingRequest = PeriodicWorkRequestBuilder<LocationTrackingWorker>(5, TimeUnit.MINUTES)
//            .setConstraints(constraints)
//            .build()
//
//        WorkManager.getInstance().enqueueUniquePeriodicWork(
//            "Location",
//            ExistingPeriodicWorkPolicy.REPLACE,
//            repeatingRequest
//        )
//    }
//
//    private fun delayedInit() {
//        applicationScope.launch {
//            setUpRecurringWork()
//        }
//    }

}