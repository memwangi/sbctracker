package com.example.sbctracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sbctracker.db.SbcTrackerDatabase
import com.example.sbctracker.models.LastLocation
import com.example.sbctracker.repository.LastLocationRepository
import com.google.android.gms.common.api.Response
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.io.IOException

class LastLocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LastLocationRepository

    // To get the saved location from Room database
    val location: MutableLiveData<LastLocation> = MutableLiveData()

    var lastLocation: LiveData<LastLocation>


    init {
        val locationDao = SbcTrackerDatabase.getDatabase(application).lastLocationDao()
        repository = LastLocationRepository(locationDao)
        lastLocation = repository._lastLocation
    }

}