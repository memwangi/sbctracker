package com.example.sbctracker

import androidx.multidex.MultiDexApplication


class TraceApplication : MultiDexApplication() {


    /**
     * onCreate is called before the first screen is shown to the user.
     * Use it to setup any background tasks, running expensive setup operations in a background
     * thread to avoid delaying app start.
     */
    override fun onCreate() {
        super.onCreate()
    }

}