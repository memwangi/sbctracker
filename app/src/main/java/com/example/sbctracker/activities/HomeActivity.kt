package com.example.sbctracker.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.sbctracker.R
import com.example.sbctracker.models.LastLocation
import com.example.sbctracker.utils.SaveSharedPreference
import com.example.sbctracker.viewmodel.LastLocationViewModel
import com.example.sbctracker.viewmodel.MachineViewModel
import com.example.sbctracker.viewmodel.UserViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.zxing.client.android.Intents
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_home.*
import org.joda.time.DateTime

class HomeActivity : AppCompatActivity() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var machineViewModel: MachineViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var latitude: String
    private lateinit var longitude: String
    private var TAG = "HomeActivity"
    private var locationUpdateState = false
    private lateinit var lastLocationViewModel: LastLocationViewModel
    private lateinit var mLastLocation: Location

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val REQUEST_PHONE_STATE = 101
        private const val REQUEST_SCAN_CONFIRM = 7
        private const val REQUEST_SCAN_NEW = 5
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)
        setSupportActionBar(app_bar)
        // Initialize view model
        machineViewModel = ViewModelProvider(this).get(MachineViewModel::class.java)
        lastLocationViewModel = ViewModelProvider(this).get(LastLocationViewModel::class.java)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        val tel = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        imeiTextView.text = tel.imei

        userViewModel.user.observe(this@HomeActivity, Observer {
            it?.let {
                this@HomeActivity.runOnUiThread {
                    supervisorText.text = it.supervisor
                    username.text = it.name
                }
            }
        })



        // Upload any cached data
        uploadCachedData()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationClient()


        // Location request
        createLocationRequest()


        //Location call back
        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                // Update location
                mLastLocation = p0?.lastLocation!!

                longitude = mLastLocation.longitude.toString()
                latitude = mLastLocation.latitude.toString()

                // Check whether IMEI is enabled
                if (ActivityCompat.checkSelfPermission(
                        this@HomeActivity,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {

                        var tel = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        var IMEI = tel.imei
                        sendLocationUpdates(mLastLocation, IMEI)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        this@HomeActivity,
                        arrayOf(Manifest.permission.READ_PHONE_STATE),
                        HomeActivity.REQUEST_PHONE_STATE
                    )
                }
            }
        }

        // Floating action button
        btnScan.setOnClickListener {

            run {
                IntentIntegrator(this@HomeActivity)
                    .setOrientationLocked(false)
                    .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                    .setRequestCode(REQUEST_SCAN_NEW)
                    .initiateScan()
            }
        }

        btnExisting.setOnClickListener {
            run {
                IntentIntegrator(this)
                    .setOrientationLocked(false)
                    .setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
                    .setRequestCode(REQUEST_SCAN_CONFIRM)
                    .initiateScan()
            }
        }

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.logout -> {
                // Set LoggedIn status to false
                SaveSharedPreference.setLoggedIn(applicationContext, false);
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        val intent = Intent(applicationContext, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }

            REQUEST_SCAN_CONFIRM -> {
                val scanIntent = Intent(this, DeviceConfirmActivity::class.java)

                val content = data?.getStringExtra(Intents.Scan.RESULT)
                if (content != null) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            HomeActivity.REQUEST_LOCATION_PERMISSION
                        )
                    }
                    val tel = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val IMEI = tel.imei

                    // Then use the longitude and latitude value for the next activity
                    scanIntent.putExtra("Result", content)
                    scanIntent.putExtra("imei", IMEI)
                    startActivity(scanIntent)


                } else {
                    Log.i("HOME", "Failed")
                }
            }


            REQUEST_SCAN_NEW -> {
                val scanIntent = Intent(this, ScanActivity::class.java)

                val content = data?.getStringExtra(Intents.Scan.RESULT)
                if (content != null) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            HomeActivity.REQUEST_LOCATION_PERMISSION
                        )
                    }
                    val tel = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    var IMEI = tel.imei
                    scanIntent.putExtra("Result", content)
                    scanIntent.putExtra("imei", IMEI)
                    startActivity(scanIntent)
                } else {
                    Log.i("HOME", "Failed")
                }
            }

        }
    }


    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    /**
     * Send location updates
     * */
    private fun sendLocationUpdates(lastLocation: Location, imei: String) {
        // Send updates
        latitude = lastLocation.latitude.toString()
        longitude = lastLocation.longitude.toString()


        val tracedLocation = LastLocation(
            0,
            lastLocation.longitude.toString(),
            lastLocation.latitude.toString(),
            imei
        )
        Log.i(TAG, tracedLocation.toString())
        // Store location
        lastLocationViewModel.insertLastLocation(tracedLocation)
        // Observe changes and send cached data
        lastLocationViewModel.lastLocation.observe(this, Observer {
            it?.let {
                lastLocationViewModel.sendUpdates(it)
            }
        })
    }

    /**
     * Set up fused location provider
     * */
    private fun setupLocationClient() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )

        }

    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 1000 * 50
        locationRequest.fastestInterval = 10000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this@HomeActivity,
                        HomeActivity.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    /**
     * Upload cached data once there's an internet connection
     * */
    private fun uploadCachedData() {
        // Observer data that hasn't been posted
        machineViewModel.allMachines.observe(this, Observer {
            //Toast.makeText(this, "Uploading cached data", Toast.LENGTH_LONG).show()
            it?.let {
                for (machine in it) {
                    machineViewModel.postNewItem(machine)
                }
            }
        })

    }


    private fun startLocationUpdates() {
        // Check whether access location permission is granted
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PHONE_STATE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Once user grants permission to access the location, get the current location
                val tel = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                var IMEI = tel.imei

                if (IMEI != null) {
                    imeiTextView.text = IMEI
                    userViewModel.refreshUser(IMEI)
                }
            }
        }

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Once user grants permission to access the location, initiate fused location client

            }
        }
    }
}
