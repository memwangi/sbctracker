package com.example.sbctracker.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "location_cache")
data class LastLocation (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val longitude: String,
    val latitude: String,
    val imei: String
)
{

}


