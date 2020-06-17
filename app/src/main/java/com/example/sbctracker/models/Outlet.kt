package com.example.sbctracker.models

import androidx.lifecycle.Transformations.map
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.joda.time.DateTime

@Entity(tableName = "table_machines", indices = [Index(value = ["barcode"], unique = true)])
data class Outlet(
    @PrimaryKey
    val id: Long = 0,
    val outletName: String,
    val phoneNumber: String,
    val description: String,
    val barcode: String,
    val outletLocation: String,
    var businessType: String,
    val longitude: String,
    val latitude: String,
    val dateScanned: Long,
    val path: String,
    val identifier: String,
    val posted: Boolean,
    val supervisorID: String
) {

}