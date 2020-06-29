package com.example.sbctracker.repository

import android.app.Application
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.sbctracker.DAO.LastLocationDao
import com.example.sbctracker.SBCLocationManager
import com.example.sbctracker.TraceApplication
import com.example.sbctracker.api.TraceNetwork
import com.example.sbctracker.api.TraceNetworkService
import com.example.sbctracker.db.SbcTrackerDatabase
import com.example.sbctracker.models.LastLocation
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class LastLocationRepository(private val locationDao: LastLocationDao, context: Context) {

    var _lastLocation = locationDao.getRecentLocation()
    private var locationManager = SBCLocationManager(context)
    /**
     * Store the recent location updates.
     * */

    // Identifier is the id of the device being tracked. It's passed through the work manager.
    fun addLocation(identifier: String) {
        locationManager.getUpdatedLocation {
            onLocationReceived(it, identifier)
        }
    }

    private fun onLocationReceived(location: Location?, identifier: String) = GlobalScope.launch {
        location?.let {
            withContext(Dispatchers.IO) {
                val tracedLocation = LastLocation(
                    0,
                    it.longitude.toString(),
                    it.latitude.toString(),
                    identifier
                )
                //Insert the last location item
                Log.i("Inserting Location","Last Location Stored")
                locationDao.insert(tracedLocation)
                sendLocationUpdate(tracedLocation)
            }
        }
    }


    suspend fun sendLocationUpdate(location: LastLocation){
        withContext(Dispatchers.IO) {
            launch {
                var security_key =
                    "WFv3dqP7oC+Od1GxnQFKwkdyf0i6ipSiTfKtARYSQShO0BwcuvPDLOizPRIiUH"

                var gson = Gson()
                var locationBody = gson.toJson(location)

                var body =
                    locationBody.toString()
                        .toRequestBody("Content-Type, application/json".toMediaTypeOrNull())

                Log.i("Uploading Location","Location object:, ${locationBody.toString()}")
                // Handle location updates
                TraceNetwork.api.postLocationUpdates(security_key,body).enqueue(object: Callback<String> {
                    override fun onFailure(call: Call<String>, t: Throwable) {
                        println("Error while sending request" + t.message)
                    }

                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        Log.i("Location Updates","""Response: ${response.code()} ${response.body().toString()}""")
                    }
                })
            }
        }
    }
}


