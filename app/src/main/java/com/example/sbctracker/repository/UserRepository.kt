package com.example.sbctracker.repository

import android.util.Log
import com.example.sbctracker.DAO.UserDao
import com.example.sbctracker.models.User


class UserRepository (private val userDao: UserDao){
    private val TAG = "User Repository"

    val user = userDao.getUserDetails()

    suspend fun insert(user: User) {
        try {
            userDao.insert(user)

        } catch (e: Exception) {
            Log.i(TAG, "User insertion failed")
        }

    }


}