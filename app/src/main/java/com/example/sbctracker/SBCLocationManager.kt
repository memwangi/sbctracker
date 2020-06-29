package com.example.sbctracker

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit

class SBCLocationManager(val context: Context) {

    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private lateinit var locationRequest: LocationRequest
    private lateinit var lastLocation: Location



    fun getUpdatedLocation(block: (Location?) -> Unit) {

        val locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Log.i("LocationMan", locationResult.lastLocation.toString())
                block.invoke(locationResult.lastLocation)
            }
        }


        locationRequest = LocationRequest()
        locationRequest.interval = TimeUnit.MINUTES.toMillis(10)
        locationRequest.fastestInterval = TimeUnit.MINUTES.toMillis(7)
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val task = fusedLocationClient.lastLocation

        task.addOnSuccessListener {
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
    }

}

