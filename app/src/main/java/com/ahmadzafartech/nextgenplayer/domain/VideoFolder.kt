package com.ahmadzafartech.nextgenplayer.domain

import android.graphics.Bitmap

data class VideoFolder(
    val name: String,
    val path: String,
    val title: String,
    val thumbnail: Bitmap? = null,
    // first video or null
    val videoCount: Int
)

