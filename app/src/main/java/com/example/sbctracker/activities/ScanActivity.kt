package com.example.sbctracker.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.sbctracker.R
import com.example.sbctracker.models.Outlet
import com.example.sbctracker.viewmodel.LastLocationViewModel
import com.example.sbctracker.viewmodel.MachineViewModel
import com.example.sbctracker.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_scan.*
import org.joda.time.DateTime
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


class ScanActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var barcode: String
    private var filePath: String? = null
    private var categoryList: MutableList<String>? = null
    private var channelList: MutableList<String>? = null
    private lateinit var identifier: String
    private lateinit var machineViewModel: MachineViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var lastLocationViewModel: LastLocationViewModel
    private lateinit var latitude: String
    private lateinit var longitude: String
    private lateinit var mProgressBar: ProgressBar
    private var customerType: String? = null
    private var channelType: String? = null
    private lateinit var supervisorID: String


    companion object {
        const val REQUEST_TAKE_PHOTO = 2

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        lastLocationViewModel = ViewModelProvider(this).get(LastLocationViewModel::class.java)

        machineViewModel = ViewModelProvider(this).get(MachineViewModel::class.java)

        lastLocationViewModel.lastLocation.observe(this, Observer {
            it?.let {
                longitude = it.longitude
                latitude = it.latitude
            }
        })
        mProgressBar = progressBar
        // Get information task details from intent
        supervisorID = intent.getStringExtra("superID")
        identifier = intent.getStringExtra("identifier")
        barcode = intent.getStringExtra("Result")
        itemIdentifier.text = barcode

        initTypeSpinner()
        initChannelSpinner()

        btnCancel.setOnClickListener {
            onBackPressed()
        }

        btnSubmit.setOnClickListener {
            // Post the item details as a new item
            if (!locationPhone.text.isNullOrEmpty() && !locationName.text.isNullOrEmpty()
                && channelType != null && customerType != null && !outletLocation.text.isNullOrEmpty()) {
                if (isPhoneValid(locationPhone.text)) {
                    // Clear the error
                    locationPhone.error = null
                    addMachine(latitude, longitude, supervisorID)
                } else {
                    locationPhone.error = "Please enter a correct phone number e.g 0712300000"
                }
            } else {
                Toast.makeText(
                    this,
                    "All fields are required. Please fill all entries.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Launch dialog to select from gallery or take photo
        uploadImgBtn.setOnClickListener {
            takePhoto()
        }

    }

    private fun initTypeSpinner() {
        categoryList = ArrayList()
        categoryList!!.add("Distributor")
        categoryList!!.add("Stockist")
        categoryList!!.add("Retailer")
        typeSpinner.item = categoryList

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                customerType = categoryList!![position]
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {
                Toast.makeText(
                    this@ScanActivity,
                    "Please select a category to continue",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun initChannelSpinner() {
        channelList = ArrayList()
        channelList!!.add("Grocery/Kiosk")
        channelList!!.add("Bar/Restaurant")
        channelList!!.add("School")
        channelList!!.add("Supermarket")
        channelList!!.add("Other")

        channelSpinner.item = channelList
        channelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                channelType = channelList!![position]
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {
                Toast.makeText(
                    this@ScanActivity,
                    "Please select a channel to continue",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isPhoneValid(text: Editable?): Boolean {
        var pattern =
            Pattern.compile("^(?:254|\\+254|0)?(7(?:(?:[0-9][0-9])|(?:0[0-8])|(?:9[0-2]))[0-9]{6})$")
        var matcher = pattern.matcher(text!!)
        var valid = false
        if (matcher.matches() && text!!.length in 10..13) {
            valid = true
        }
        return valid
    }


    override fun onBackPressed() {
        finish()
        super.onBackPressed()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            checkReadPermissions()
            // Fetch selected image
            Glide.with(this).load(filePath).fitCenter().into(uploadImage);
        }
    }


    private fun addMachine(latitude: String, longitude: String, supervisorID: String) {

        var dateTimeNow = DateTime.now()

        // Stores time in milliseconds since epoch for accuracy
        if (!filePath.isNullOrBlank()) {
            var machine = filePath?.let {
                channelType?.let { channelType ->
                    customerType?.let { customerType ->
                        Outlet(
                            0,
                            locationName.editableText.toString(),
                            locationPhone.editableText.toString(),
                            channelType,
                            barcode,
                            outletLocation.editableText.toString(),
                            customerType,
                            longitude,
                            latitude,
                            dateTimeNow.millis,
                            it,
                            identifier,
                            false,
                            supervisorID
                        )
                    }
                }

            }

            // Create new machine
            if (machine != null) {

                var connectivityManager =
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo

                if (activeNetwork?.isConnected != null) {
                    progressBar.visibility = View.VISIBLE
                    machineViewModel.insert(machine)
                    machineViewModel.postNewItem(machine)
                    machineViewModel.toastMesage.observe(this, Observer { it ->
                        it.getContentIfNotHandled()?.let {
                            if (it.containsKey(true)) {
                                Toast.makeText(this, it[true], Toast.LENGTH_LONG)
                                    .show()
                                uploadImage.setImageResource(R.drawable.ic_camera)
                                locationName.text = null
                                locationPhone.text = null
                                progressBar.visibility = View.GONE
                                onBackPressed()
                            } else {
                                uploadImage.setImageResource(R.drawable.ic_camera)
                                locationName.text = null
                                locationPhone.text = null
                                Toast.makeText(
                                    this,
                                    it[false],
                                    Toast.LENGTH_LONG
                                ).show()
                                onBackPressed()
                            }

                        }
                    })
                } else {
                    Toast.makeText(
                        this,
                        "Please check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }


            }

        } else {
            Toast.makeText(
                this@ScanActivity,
                "Please select or add a photo",
                Toast.LENGTH_LONG
            ).show()
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

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        TODO("Not yet implemented")
    }

}



