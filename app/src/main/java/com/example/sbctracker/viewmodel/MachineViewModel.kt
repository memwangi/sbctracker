package com.example.sbctracker.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sbctracker.Event
import com.example.sbctracker.db.SbcTrackerDatabase
import com.example.sbctracker.models.Outlet
import com.example.sbctracker.repository.MachineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.joda.time.DateTime
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class MachineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MachineRepository
    val allMachines: LiveData<List<Outlet>>
    val client = OkHttpClient()
    private val _toastMessage = MutableLiveData<Event<HashMap<Boolean, String>>>()

    val toastMesage: LiveData<Event<HashMap<Boolean, String>>>
        get() = _toastMessage

    init {
        val machineDao = SbcTrackerDatabase.getDatabase(application).machineDao()
        repository = MachineRepository(machineDao)
        allMachines = repository.allMachines
    }

    fun insert(outlet: Outlet) = viewModelScope.launch {
        withContext(IO) {
            try {
                // Insert new location
                Log.i("Inserting machine", outlet.phoneNumber)
                repository.insert(outlet)

            } catch (e: IOException) {
                Log.i("Insert Machine", e.toString())
            }
        }

    }

    fun setPosted(outlet: Outlet) {
        viewModelScope.launch(IO) {
            try {
                repository.setPosted(outlet.id)
                Log.i("Marked posted", "MARKED POSTED")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    fun postNewItem(outlet: Outlet) {
        viewModelScope.launch(IO) {
            var stream: ByteArrayOutputStream = ByteArrayOutputStream()
            var options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            // Read bitmap by filepath
            var bitmap = BitmapFactory.decodeFile(outlet.path, options)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            var bytes: ByteArray = stream.toByteArray()

            // Serialize machine
            var serialized = serializeMachine(outlet)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("data", serialized)
                .addFormDataPart(
                    "image", "${outlet.barcode}.jpg",
                    bytes.toRequestBody("image/*jpg".toMediaTypeOrNull())
                ).build()

            val request = Request.Builder()
                .url("http://165.22.51.149:4000/push/tasks")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("Response", "Failed Post Request: ${e.message}")
                    var map = HashMap<Boolean, String>()
                    map[false] = "Failed to upload data. Please check your network and try again."
                    requestMessage(map)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.code == 200) {
                        Log.i("Response", "Response is this: ${response}")
                        var map = HashMap<Boolean, String>()
                        map[true] = "Upload successful"
                        requestMessage(map)

                        // I found this method necessary because there was no way to know whether
                        // an item has been posted from my end without having to make an expensive call.
                        // So once an item is posted successfully, we mark it as posted.
                        setPosted(outlet)


                    } else if (response.code == 400 || response.code == 401) {
                        Log.i("Response Failed", response.message)
                        var map = HashMap<Boolean, String>()
                        map[true] = "Failed. Item already exists"
                        requestMessage(map)
                    }
                }
            })
        }
    }

    fun updateStockImage(filepath: String, barcode: String, data: String) {

        var image = convertFilePathToByteArray(filepath)
        Log.i("Response", "Doctary: ${data}")


        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("data", data)
                    .addFormDataPart(
                        "image", "${barcode}.jpg",
                        image.toRequestBody("image/*jpg".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("http://165.22.51.149:4000/push/tasks")
                    .put(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.i("Response", "Failed Post Request: ${e.message}")
                        var map = HashMap<Boolean, String>()
                        map[true] =
                            "Failed to upload data. Please check your network and try again."
                        requestMessage(map)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.i("Update stock image", "Response is this: ${response}")
                        var map = HashMap<Boolean, String>()
                        when (response.code) {
                            200 -> {
                                map[true] = "Upload successful"
                                requestMessage(map)
                            }
                            403 -> {
                                map[false] = "This outlet has already been scanned today."
                                requestMessage(map)
                            }
                            else -> {
                                map[false] = "Failed. Please try again."
                                requestMessage(map)
                            }
                        }
                    }
                })
            }
        }
    }


    fun serializeMachine(outlet: Outlet): String {
        var map = HashMap<String, Any>()
        var date = DateTime(outlet.dateScanned)
        map["id"] = outlet.id
        map["name"] = outlet.outletName
        map["phone"] = outlet.phoneNumber
        map["type"] = outlet.businessType
        map["description"] = outlet.description
        map["barcode"] = outlet.barcode
        map["location"] = outlet.outletLocation
        map["longitude"] = outlet.longitude
        map["latitude"] = outlet.latitude
        map["date"] = date
        map["IMEI"] = outlet.identifier
        map["id"] = outlet.supervisorID
        var jsonObject = JSONObject(map)
        return jsonObject.toString()
    }

    fun convertFilePathToByteArray(filepath: String): ByteArray {
        // Image will be sent as a new Byte Array Stream
        var stream: ByteArrayOutputStream = ByteArrayOutputStream()
        var options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        // Read bitmap by filepath
        var bitmap = BitmapFactory.decodeFile(filepath, options)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
        var bytes: ByteArray = stream.toByteArray()

        return bytes
    }

    fun requestMessage(message: HashMap<Boolean, String>) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _toastMessage.setValue(Event(message))   // Trigger the event by setting a new Event as a new value
            }
        }
    }


}