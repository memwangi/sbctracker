package com.example.sbctracker.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.sbctracker.R
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_scan.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ScanActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val gson = Gson()
    private var barcode = ""
    private val PICK_IMAGE_REQUEST = 71
    private var filePath: String = ""
    var coordinates = HashMap<String, Any?>()
    var REQUEST_IMAGE_CAPTURE = 1
    var REQUEST_TAKE_PHOTO = 1
    lateinit var currentPhotoPath: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Set toolbar

        var intent = intent
        barcode = intent.getStringExtra("Result")

        coordinates["Latitude"] = intent.getStringExtra("latitude")
        coordinates["Longitude"] = intent.getStringExtra("longitude")

        var locationDetails = gson.toJson(coordinates)

        // Check if scan is successfull and get details of the item
        if (locationDetails != null) {
            getItemDetails(barcode, locationDetails)
        }

        btnSubmit.setOnClickListener {
            // Post the item details as a new item
            var data = getFormDetails(intent.getStringExtra("latitude"), intent.getStringExtra("longitude"))
            PostNewItem(data)
        }

        // Select Image
        addImage.setOnClickListener { launchGallery() }

        takePhoto.setOnClickListener {
            dispatchTakePictureIntent()
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if(data == null || data.data == null){
                return
            }
            var data1 = data.data
            filePath = data1.toString()

            var imagePath = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imagePath)
                uploadImage.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        //Image capture by taking photo
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras!!.get("data") as Bitmap

            // Inflate image to imageView
            uploadImage.setImageBitmap(imageBitmap)
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    null
                }
                // Continue if the file was successfully created
                photoFile?.also {
                    val photoUri: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.sbctracker.fileprovider",
                        photoFile
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }

    // Create image file to store in phone storage
    // Updated the manifest so that the images can only stay native to the app
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageName = "JPEG_${timestamp}_"

        var image = File.createTempFile(imageName, ".jpg", storageDir)

        currentPhotoPath = image.absolutePath
        return  image
    }

    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(currentPhotoPath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    private fun launchGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    // Gets the results from the url and parses them into JSON
    private fun getItemDetails(barcode: String, locationDetails: String) {
        val baseUrl = "http://165.22.51.149:3000/push/barcode/${barcode}"

        val request = Request.Builder().url(baseUrl).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                var result = response?.body?.string()
                var jsonObject = JSONObject(result)
                var response = jsonObject.getBoolean("response")

                this@ScanActivity.runOnUiThread(java.lang.Runnable {
                    if (response) {
                        // If there is a response, update the UI with details of the scanned item
                        var resultData = jsonObject.getJSONObject("data")
                        itemDescription.setText(resultData.getString("description"))
                        itemIdentifier.setText(resultData.getString("code"))
                        itemName.setText(resultData.getString("name"))

                        var longitude = resultData.getString("longitude")
                        // Check item location from the scanned results
                        if (longitude != null) {
                            // If item already has a location update it's details
                            updateItem(locationDetails, barcode)
                        }
                    } else {
                        location.setText(getString(R.string.ItemNotFound))
                        itemIdentifier.setText(barcode)

                    }

                })
            }
        })
    }

    // Gets the results from the url and parses them into JSON
    private fun updateItem(coordinates: String, barcode: String) {
        val formBody = FormBody.Builder()
            .add("location", coordinates)
            .build()
        val request = Request.Builder()
            .url("http://165.22.51.149:3000/push/barcode/${barcode}")
            .put(formBody)
            .build()
        client.newCall(request).enqueue( object: Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                println("Failed to post request")
            }

            override fun onResponse(call: Call, response: Response) {
                var result = response.body?.string()
                println(result)
            }

        })

    }

    private fun PostNewItem(itemDetails: String) {

        val file = File(filePath.toString())
        val image = file.asRequestBody("image/png".toMediaTypeOrNull())
        if (filePath != null) {

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("data", itemDetails)
                .addFormDataPart("file", filePath.toString(), image)
                .build()

            val request = Request.Builder()
                .url("http://165.22.51.149:3000/push/barcode/")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    println(response.body?.string())
                }
            })
        } else {
            Toast.makeText(this@ScanActivity, "Not working", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFormDetails(latitude: String, longitude: String): String {
        // Get item details
        var postDetails = HashMap<String,Any?>()

        postDetails["name"] = itemName.editableText.toString()
        postDetails["description"] = itemDescription.editableText.toString()
        postDetails["code"] = itemIdentifier.editableText.toString()
        postDetails["latitude"] = latitude
        postDetails["longitude"] = longitude

        var jsonified = JSONObject(postDetails)
        return jsonified.toString()

    }

//    private fun uploadImage() {
//        if (filePath != null) {
//            val file = File(filePath.toString())
//            val image = file.asRequestBody("image/png".toMediaTypeOrNull())
//
//            val requestBody = MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("barcode", barcode)
//                .addFormDataPart("file", filePath.toString(), image)
//                .build()
//
//            val request = Request.Builder()
//                .url("http://165.22.51.149:3000/push/barcode/")
//                .post(requestBody)
//                .build()
//
//            client.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    println("Failed to send image")
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    Toast.makeText(this@ScanActivity, "working", Toast.LENGTH_LONG).show()
//                }
//            })
//        }
//
//        }

}



