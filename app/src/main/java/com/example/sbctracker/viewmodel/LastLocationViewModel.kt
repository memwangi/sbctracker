package com.example.sbctracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.sbctracker.db.SbcTrackerDatabase
import com.example.sbctracker.models.LastLocation
import com.example.sbctracker.repository.LastLocationRepository
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.io.IOException

class LastLocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LastLocationRepository

    var lastLocation: LiveData<LastLocation>

    init {
        val locationDao = SbcTrackerDatabase.getDatabase(application).lastLocationDao()
        repository = LastLocationRepository(locationDao)
        lastLocation = repository.lastLocation
    }


    fun insertLastLocation(lastLocation: LastLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Insert new location
                repository.addLocation(lastLocation)
                Log.i("Insert Location","Location stored successfully")

            } catch (e: IOException) {
                Log.i("Insert Location",e.toString())
            }
        }
    }

    fun sendUpdates(location: LastLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.sendLocationUpdate(location)
                Log.i("Sending Cached Data", "Sent")
            } catch (e: HttpException) {
                Log.i("Sending Cached Data", "Error")
            }

        }
    }


}