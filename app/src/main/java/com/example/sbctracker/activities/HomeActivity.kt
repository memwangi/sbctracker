package com.example.sbctracker.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.sbctracker.R
import com.example.sbctracker.utils.SaveSharedPreference
import com.example.sbctracker.utils.UniqueDeviceID
import com.example.sbctracker.viewmodel.LastLocationViewModel
import com.example.sbctracker.viewmodel.MachineViewModel
import com.example.sbctracker.viewmodel.UserViewModel
import com.example.sbctracker.work.LocationWorker
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.zxing.client.android.Intents
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_home.*
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var machineViewModel: MachineViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var supervisorID: String
    private var mContext: Context? = null
    private var TAG = "HomeActivity"
    private lateinit var mProgressBar: ProgressBar
    private var locationUpdateState = false
    private lateinit var lastLocationViewModel: LastLocationViewModel
    private lateinit var identifier: String


    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
        const val REQUEST_CHECK_SETTINGS = 2
        private const val REQUEST_PHONE_STATE = 101
        private const val REQUEST_SCAN_CONFIRM = 7
        private const val REQUEST_SCAN_NEW = 5
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        supportActionBar?.setDisplayShowHomeEnabled(true);
        supportActionBar?.setLogo(R.mipmap.gnl)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        // Initialize view model
        machineViewModel = ViewModelProvider(this).get(MachineViewModel::class.java)
        lastLocationViewModel = ViewModelProvider(this).get(LastLocationViewModel::class.java)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        mProgressBar = progressBarHome
        mContext = applicationContext

        // Get ID from shared preferences
        var id = UniqueDeviceID.getID(this)
        id?.let {
            identifier = it
        }
        userViewModel.user.observe(this@HomeActivity, Observer {
            it?.let {
                // Identifier is the phone number. Used to identify the user/device when the imei is not available.
                this@HomeActivity.runOnUiThread {
                    supervisorID = it.id.toString()
                    supervisorText.text = it.supervisor
                    username.text = it.name
                    imeiTextView.text = identifier
                }

            }
        })

        // Once user details are available, then start transmitting data
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Request location permission and start worker
        requestLocationPermission(identifier)



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
                logout()
                true
            }

            R.id.refresh -> {

                mProgressBar.visibility = View.VISIBLE
                Handler().postDelayed({
                    requestLocationPermission(identifier)
                }, 3000)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    private fun logout() {
        BackgroundTracker.stopWorker()
        userViewModel.logout(identifier)
        userViewModel.toastMessage.observe(this, Observer { it ->
            it.getContentIfNotHandled()?.let {
                if (it.first) {
                    // Set LoggedIn status to false
                    SaveSharedPreference.setLoggedIn(applicationContext, false)
                    val intent = Intent(applicationContext, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        applicationContext, "Failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun switchOnWorker() = mContext?.let {
        BackgroundTracker.startWorker(
        identifier, progressBar = mProgressBar, context = it,
        logout = ::logout
    )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                switchOnWorker()

            }

            REQUEST_SCAN_CONFIRM -> {
                val scanIntent = Intent(this, DeviceConfirmActivity::class.java)

                val content = data?.getStringExtra(Intents.Scan.RESULT)
                if (content != null) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.READ_PHONE_STATE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_PHONE_STATE),
                            HomeActivity.REQUEST_PHONE_STATE
                        )
                    }
                    // Then use the longitude and latitude value for the next activity
                    scanIntent.putExtra("Result", content)
                    scanIntent.putExtra("identifier", identifier)
                    scanIntent.putExtra("superID", supervisorID)
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
                            android.Manifest.permission.READ_PHONE_STATE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_PHONE_STATE),
                            HomeActivity.REQUEST_PHONE_STATE
                        )
                    }
                    scanIntent.putExtra("superID", supervisorID)
                    scanIntent.putExtra("Result", content)
                    scanIntent.putExtra("identifier", identifier)
                    startActivity(scanIntent)
                } else {
                    Log.i("HOME", "Failed")
                }
            }
        }
    }


    /**
     * Set up fused location provider
     * */
    private fun requestLocationPermission(identifier: String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            turnOnGPS(identifier)

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }

    }

    private fun turnOnGPS(identifier: String) {
        locationRequest = LocationRequest()
        var task = LocationServices.getSettingsClient(this)
            .checkLocationSettings(
                LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                    .setAlwaysShow(true)
                    .build()
            )

        task.addOnSuccessListener {
            switchOnWorker()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Ask user to turn location
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this@HomeActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error
                }
            }
        }
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

                imeiTextView.text = identifier
                userViewModel.refreshUser(identifier)
            }
        }

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                turnOnGPS(identifier)
            }
        }
    }

    object BackgroundTracker {
        val WORKER_TAG = "Location Worker"


        fun isWorkScheduled(workInfos: List<WorkInfo>?): Boolean {
            var running = false
            if (workInfos == null || workInfos.isEmpty()) return false
            for (workStatus in workInfos) {
                running =
                    workStatus.state == WorkInfo.State.RUNNING || workStatus.state == WorkInfo.State.ENQUEUED
            }
            return running
        }

        inline fun startWorker(
            identifier: String,
            progressBar: ProgressBar,
            context: Context,
            logout: () -> Unit
        ) {
            val dateTimeNow = DateTime.now()
            val hour = dateTimeNow.hourOfDay().get()
            // Can only run from 8 till 6 in the evening.
            if (hour in 1..23) {
                // Incase user hit refresh
                progressBar.visibility = View.GONE
                //Start tracking location updates
                if (SaveSharedPreference.getLoggedStatus(context)) {
                    if (!isWorkScheduled(
                            WorkManager.getInstance().getWorkInfosByTag(WORKER_TAG).get()
                        )
                    ) {
                        //If there is now work scheduled then do
                        val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()

                        val data = Data.Builder()
                        data.putString("identifier", identifier)

                        val repeatingRequest =
                            PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
                                .setConstraints(constraints)
                                .setBackoffCriteria(
                                    BackoffPolicy.LINEAR,
                                    PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                                    TimeUnit.MILLISECONDS
                                )
                                .setInputData(data.build())
                                .build()

                        WorkManager.getInstance().enqueueUniquePeriodicWork(
                            WORKER_TAG,
                            ExistingPeriodicWorkPolicy.REPLACE,
                            repeatingRequest
                        )
                    }
                }
            } else {
                logout()
            }
        }

        fun stopWorker() {
            WorkManager.getInstance().cancelAllWorkByTag(WORKER_TAG)
        }

    }
}


