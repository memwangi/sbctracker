package com.example.sbctracker.viewmodel

import android.app.Application
import android.text.Editable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sbctracker.Event
import com.example.sbctracker.api.TraceNetwork
import com.example.sbctracker.db.SbcTrackerDatabase
import com.example.sbctracker.models.User
import com.example.sbctracker.repository.UserRepository
import com.google.gson.Gson
import com.squareup.moshi.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserRepository
    val user: LiveData<User>

    private val _toastMessage = MutableLiveData<Event<Boolean>>()

    val toastMesage: LiveData<Event<Boolean>>
        get() = _toastMessage

    init {
        val userDao = SbcTrackerDatabase.getDatabase(application).UserDao()
        repository = UserRepository(userDao)
        user = repository.user
    }

    fun insert(user: User) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                // Insert new location
                Log.i("Inserting User", user.supervisor)
                repository.insert(user)

            } catch (e: IOException) {
                Log.i("Insert user", e.toString())
            }
        }

    }


    fun refreshUser(imei: String) {
        viewModelScope.launch(IO) {
            var security_key =
                "WFv3dqP7oC+Od1GxnQFKwkdyf0i6ipSiTfKtARYSQShO0BwcuvPDLOizPRIiUH"
            var userObject = hashMapOf<String, String>()
            userObject["IMEI"] = imei

            var gson = Gson()
            var userBody = gson.toJson(userObject)


            var body =
                userBody.toString()
                    .toRequestBody("Content-Type, application/json".toMediaTypeOrNull())

            try {
                var data = TraceNetwork.api.getUserDetails(security_key, body).await()
                Log.i("TAG", data.phone)
                var user = User(
                    data.id,
                    data.name,
                    data.supervisor,
                    data.phone,
                    data.active
                )
                Log.i("User Info", user.toString())
                insert(user)

            } catch (e: HttpException) {
                Log.i("Home Activity", "Device Not Registered")
            }


        }
    }

    fun userLogin(phone: String, password: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var security_key =
                    "WFv3dqP7oC+Od1GxnQFKwkdyf0i6ipSiTfKtARYSQShO0BwcuvPDLOizPRIiUH"
                var userObject = hashMapOf<String, Any>()
                userObject["phone"] = phone.toInt()
                userObject["password"] = password

                var gson = Gson()
                var userBody = gson.toJson(userObject)

                var body =
                    userBody.toString()
                        .toRequestBody("Content-Type, application/json".toMediaTypeOrNull())

                // Handle location updates
                TraceNetwork.api.login(security_key,body).enqueue(object: Callback<String>{
                    override fun onFailure(call: Call<String>, t: Throwable) {
                        println("Error while sending request" + t.message)

                    }

                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        Log.i("User login","""Response: ${response.code()} ${response.body().toString()}""")
                        if (response.code() == 200) {
                            requestMessage(true)

                        }
                        else { requestMessage(false)
                        }

                    }

                })
            }
        }
    }

    fun logout(phone: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var security_key =
                    "WFv3dqP7oC+Od1GxnQFKwkdyf0i6ipSiTfKtARYSQShO0BwcuvPDLOizPRIiUH"
                var userObject = hashMapOf<String, Any>()
                userObject["phone"] = phone.toInt()

                var gson = Gson()
                var userBody = gson.toJson(userObject)

                var body =
                    userBody.toString()
                        .toRequestBody("Content-Type, application/json".toMediaTypeOrNull())

                // Handle location updates
                TraceNetwork.api.logout(security_key,body).enqueue(object: Callback<String>{
                    override fun onFailure(call: Call<String>, t: Throwable) {
                        println("Error while sending request" + t.message)

                    }

                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        Log.i("User login","""Response: ${response.code()} ${response.body().toString()}""")
                        if (response.code() == 200) {
                            requestMessage(true)

                        }
                        else { requestMessage(false)
                        }

                    }

                })
            }
        }
    }

    fun requestMessage(message: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _toastMessage.setValue(Event(message))   // Trigger the event by setting a new Event as a new value
            }
        }
    }
}