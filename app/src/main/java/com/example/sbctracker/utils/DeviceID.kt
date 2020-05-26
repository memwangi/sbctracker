package com.example.sbctracker.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Nullable
import java.util.*


object UniqueDeviceID {
    var uniqueID: String? = null
    val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"


    @Nullable
    fun setID(context: Context, phone: String) {
        val sharedPrefs: SharedPreferences = context.getSharedPreferences(
            PREF_UNIQUE_ID, Context.MODE_PRIVATE
        )
        uniqueID = phone
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        editor.putString(PREF_UNIQUE_ID, uniqueID)
        editor.apply()
    }

    fun getID(context: Context): String? {
        val sharedPrefs: SharedPreferences = context.getSharedPreferences(
            PREF_UNIQUE_ID, Context.MODE_PRIVATE
        )
        uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null)
        return uniqueID
    }
}