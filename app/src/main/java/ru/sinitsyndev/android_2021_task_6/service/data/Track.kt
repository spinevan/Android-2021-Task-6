package ru.sinitsyndev.android_2021_task_6.service.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Track(
    val title: String,
    val artist: String,
    val bitmapUri: String,
    val trackUri: String,
    val duration: Int
)
