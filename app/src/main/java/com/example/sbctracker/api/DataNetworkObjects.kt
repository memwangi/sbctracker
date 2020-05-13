package com.example.sbctracker.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkUser(
    val supervisor: String,
    val phone: String,
    val name: String,
    val id: Long,
    val active: String
)