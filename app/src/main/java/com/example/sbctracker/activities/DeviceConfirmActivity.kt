package com.example.sbctracker.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.sbctracker.R
import com.example.sbctracker.viewmodel.LastLocationViewModel
import com.example.sbctracker.viewmodel.MachineViewModel
import com.example.sbctracker.viewmodel.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_device_confirm.*
import kotlinx.android.synthetic.main.activity_device_confirm.btnCancel
import kotlinx.android.synthetic.main.activity_device_confirm.btnSubmit
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_scan.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class DeviceConfirmActivity : AppCompatActivity() {

    private lateinit var barcode: String
    private val client = OkHttpClient()
    private lateinit var latitude: String
    private lateinit var longitude: String
    private lateinit var IMEI: String
    private lateinit var machineViewModel: MachineViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var lastLocationViewModel: LastLocationViewModel
    private var filePath: String? = null
    private lateinit var mProgressBar: ProgressBar
    var REQUEST_TAKE_PHOTO = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_confirm)
        // Initialize text fields
        mProgressBar = confirmProgressBar
        machineViewModel = ViewModelProvider(this).get(MachineViewModel::class.java)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        lastLocationViewModel = ViewModelProvider(this).get(LastLocationViewModel::class.java)

        lastLocationViewModel.lastLocation.observe(this, Observer {
            it?.let {
                longitude = it.longitude
                latitude = it.latitude
            }
        })
        barcode = intent.getStringExtra("Result")
        IMEI = intent.getStringExtra("imei")

        // If this intent carries taskDetails


        userViewModel.user.observe(this@DeviceConfirmActivity, Observer {
            it?.let {

                var data = serializeData(barcode, latitude, longitude, IMEI, it.id)
                if (checkNetworkConnectivity()) {
                    getItemDetails(barcode, data)
                } else {
                    notFoundTextView.setText("Failed to connect. Please check your internet and try again.")
                    notFoundTextView.visibility = View.VISIBLE
                }

            }
        })

        btnCancel.setOnClickListener {
            onBackPressed()
        }

        btnSubmit.setOnClickListener {

            // Post the item details as a new item
            if (filePath != null) {

                mProgressBar.visibility = View.VISIBLE
                machineViewModel.updateStockImage(filePath!!, barcode, IMEI)

                machineViewModel.toastMesage.observe(this, Observer { it ->
                    it.getContentIfNotHandled()?.let {
                        if (it) {
                            Toast.makeText(this, "Update successful", Toast.LENGTH_LONG).show()
                            mProgressBar.visibility = View.GONE
                            onBackPressed()
                        } else {
                            mProgressBar.visibility = View.GONE
                            Toast.makeText(
                                this,
                                "Failed.",
                                Toast.LENGTH_LONG
                            ).show()
                            onBackPressed()
                        }
                    }
                })
            } else {
                Toast.makeText(
                    this,
                    "Please add an image to continue",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Launch dialog to select from gallery or take photo
        uploadImageBtn.setOnClickListener {
            takePhoto()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            checkReadPermissions()
            // Fetch selected image
            Glide.with(this).load(filePath).fitCenter().into(coolerImage);
        }
    }


    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var capturedImage: File? = null
            try {
                capturedImage = createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            if (capturedImage != null) {
                var photoURI =
                    FileProvider.getUriForFile(
                        this,
                        "com.example.sbctracker.provider",
                        capturedImage
                    );
                takePictureIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    photoURI
                );
                startActivityForResult(
                    takePictureIntent,
                    REQUEST_TAKE_PHOTO
                );
            }
        }
    }

    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val mFileName = "JPEG_${barcode}" + timeStamp + "_"
        val storageDir =
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        var image: File = File.createTempFile(mFileName, ".jpg", storageDir)
        filePath = image.getAbsolutePath();
        return image;
    }

    // Gets the item details whose barcode has been scanned from the url and parses them into JSON
// If item not found, prompts the user to enter details manually
    private fun getItemDetails(barcode: String, data: String) {
        val baseUrl = "http://165.22.51.149:4000/push/barcode/${barcode}"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("data", data)
            .build()
        val request = Request.Builder().url(baseUrl).put(requestBody).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
                this@DeviceConfirmActivity.runOnUiThread {

                    allDetails.visibility = View.GONE
                    notFoundTextView.visibility = View.VISIBLE
                    notFoundTextView.setText("Failed. Please check your connection and try again.")
                }

            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                var result = response.body!!.string()
                Log.i("Device Confirm", result)
                var jsonObject = JSONObject(result)
                var _response = jsonObject.getBoolean("response")

                if(_response && response.code == 200) {
                    this@DeviceConfirmActivity.runOnUiThread {
                        // If there is a response, update the UI with details of the scanned item
                        var resultData = jsonObject.getJSONObject("data")
                        Log.i("Device Confirm", "${resultData}")
                        confirmDescription.setText(resultData.getString("description"))
                        barcodeConfirm.text = barcode
                        customerType.text = resultData.getString("type")
                        confirmPhone.text = resultData.getString("phone")
                        confirmName.text = resultData.getString("name")
                    }
                } else {
                    this@DeviceConfirmActivity.runOnUiThread {
                        allDetails.visibility = View.GONE
                        notFoundTextView.visibility = View.VISIBLE
                        notFoundTextView.setText(R.string.ItemDoesNotExist)

                    }
                }
            }
        })
    }

    private fun serializeData(
        barcode: String,
        latitude: String,
        longitude: String,
        imei: String,
        id: Long
    ): String {
        var map = HashMap<String, Any>()

        map["barcode"] = barcode
        map["latitude"] = latitude
        map["longitude"] = longitude
        map["imei"] = imei
        map["id"] = id

        var jsonObject = JSONObject(map)
        return jsonObject.toString()
    }

    private fun checkReadPermissions(): Boolean {
        var ps = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        var rc = ActivityCompat.checkSelfPermission(this, ps[0])
        if (rc != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, ps, 0)
            return true
        } else {
            return false
        }
    }

    private fun checkNetworkConnectivity(): Boolean {
        var connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo

        // True or false
        return activeNetwork?.isConnected != null
    }

}
