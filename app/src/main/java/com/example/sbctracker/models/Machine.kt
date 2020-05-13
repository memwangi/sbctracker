package com.example.sbctracker.models

import androidx.lifecycle.Transformations.map
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.joda.time.DateTime

@Entity(tableName = "table_machines", indices = [Index(value = ["barcode"], unique = true)])
data class Machine(
    @PrimaryKey
    val id: Long = 0,
    val locationName: String,
    val phoneNumber: String,
    val description: String,
    val barcode: String,
    var businessType: String,
    val longitude: String,
    val latitude: String,
    val dateScanned: Long,
    val path: String,
    val IMEI: String,
    val posted: Boolean,
    val supervisorID: String
) {

}