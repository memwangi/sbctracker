package com.example.sbctracker.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "table_user")
data class User(
    @PrimaryKey
    val id: Long = 0,
    val name: String,
    val supervisor: String,
    val phone: String,
    val active: String
) {

}