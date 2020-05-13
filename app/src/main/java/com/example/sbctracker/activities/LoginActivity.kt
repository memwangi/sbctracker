package com.example.sbctracker.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.Editable
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.sbctracker.R
import com.example.sbctracker.utils.SaveSharedPreference
import com.example.sbctracker.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_login.*
import java.util.regex.Pattern


class LoginActivity : AppCompatActivity() {
    private lateinit var userViewModel: UserViewModel

    companion object {
        private const val REQUEST_PHONE_STATE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        getIMEI()

        // Check if UserResponse is Already Logged In
        if (SaveSharedPreference.getLoggedStatus(applicationContext)) {
            val intent = Intent(applicationContext, HomeActivity::class.java)
            startActivity(intent)
        } else {
            loginForm.setVisibility(View.VISIBLE)
        }

        login.setOnClickListener {
            // Post the item details as a new item
            if (!userphone.text.isNullOrEmpty() and !password_edit_text.text.isNullOrEmpty()) {
                if (isPhoneValid(userphone.text)) {
                    // Clear the error
                    userphone.error = null
                    userViewModel.userLogin(
                        userphone.text.toString(),
                        password_edit_text.text.toString()
                    )
                    userViewModel.toastMesage.observe(this, Observer { it ->
                        it.getContentIfNotHandled()?.let {
                            if (it) {
                                // Set Logged In statue to 'true'
                                SaveSharedPreference.setLoggedIn(applicationContext, true)
                                val intent =
                                    Intent(applicationContext, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    applicationContext, "Credentials are not Valid.",
                                    Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    })


                } else {
                    userphone.error = "Please enter a correct phone number e.g 0712300000"
                }

            } else {
                Toast.makeText(
                    this,
                    "All fields are required. Please fill all entries.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    }


    private fun getIMEI() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                LoginActivity.REQUEST_PHONE_STATE
            )
        } else {
            val tel = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val IMEI = tel.imei

            var connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo

            if (activeNetwork?.isConnected != null) {
                IMEI?.let {
                    userViewModel.refreshUser(it)
                }
            } else {
                Toast.makeText(
                    this,
                    "Please connect to an active network to continue",
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
}

