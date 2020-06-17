package com.example.sbctracker.work

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.sbctracker.db.SbcTrackerDatabase
import com.example.sbctracker.models.LastLocation
import com.example.sbctracker.repository.LastLocationRepository
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import retrofit2.HttpException
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class LocationTrackingWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var mContext = context
    private var locationUpdateState = false
    private lateinit var locationManager: LocationManager
    private lateinit var mLastLocation: Location
    private val TAG = "Location Worker"
    private var dateTimeNow = DateTime.now()


    override fun doWork(): Result {
        Log.d(TAG, "doWork: Done")
        Log.d(TAG, "onStartJob: STARTING JOB..")


        // Repository
        var identifier = inputData.getString("identifier")
        val locationDao = SbcTrackerDatabase.getDatabase(applicationContext).lastLocationDao()
        val repository = LastLocationRepository(locationDao)
        val hour = dateTimeNow.hourOfDay().get()
        // Can only run from 8 till 6 in the evening.
        if(hour in 7..18) {


            try {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        // Update location
                        var location = locationResult.locations
                        mLastLocation = location.last()
                        // mLastLocation = locationResult.lastLocation!!
                        val name = getCompleteAddressString(
                            mLastLocation.latitude,
                            mLastLocation.longitude
                        )
                        if (identifier != null) {
                            sendLocationUpdates(mLastLocation, identifier, repository, name)
                            location.clear()
                        }
                        Log.i(TAG, "You are at $name")
                    }
                }

                locationRequest = LocationRequest()
                locationRequest.interval = TimeUnit.MINUTES.toMillis(10)
                locationRequest.fastestInterval = TimeUnit.MINUTES.toMillis(5)
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

                val task = fusedLocationClient.lastLocation

                task.addOnSuccessListener {
                    locationUpdateState = true
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
                task.addOnFailureListener { e ->
                    if (e is ResolvableApiException) {
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            e.resolution
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // Ignore the error
                        }
                    }
                }
            } catch (e: HttpException) {
                return Result.retry()
            }
        }
        return Result.success()
    }

    private fun getCompleteAddressString(
        LATITUDE: Double,
        LONGITUDE: Double
    ): String {
        var strAdd = ""
        val geocoder = Geocoder(mContext, Locale.getDefault())
        try {
            val addresses: List<Address>? =
                geocoder.getFromLocation(LATITUDE, LONGITUDE, 1)
            if (addresses != null) {
                val returnedAddress: Address = addresses[0]
                val strReturnedAddress = StringBuilder()
                for (i in 0..returnedAddress.maxAddressLineIndex) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n")
                }
                strAdd = strReturnedAddress.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strAdd
    }

    private fun sendLocationUpdates(
        lastLocation: Location,
        imei: String,
        repository: LastLocationRepository,
        name: String
    ) {

        val tracedLocation = LastLocation(
            0,
            lastLocation.longitude.toString(),
            lastLocation.latitude.toString(),
            imei,
            name
        )
        Log.i(TAG, tracedLocation.toString())
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Insert new location
                    repository.addLocation(tracedLocation)
                    Log.i("Insert Location", "Location stored successfully")

                    //Send location updates
                    try {
                        repository.sendLocationUpdate(tracedLocation)
                        Log.i("Sending Cached Data", "Sent")
                    } catch (e: HttpException) {
                        Log.i("Sending Cached Data", "Error")
                    }

                } catch (e: IOException) {
                    Log.i("Insert Location", e.toString())
                }
            }
        }


    }
}