package com.ahmadzafartech.nextgenplayer.domain

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val uri: Uri,
    val title: String,
    val thumbnail: Bitmap? = null,
    val duration: Long? = null,
    val dateAdded: Long = 0L,   // ✅ REQUIRED for sort
    val width: Int = 0,         // ✅ for 4K
    val height: Int = 0,        // ✅ for 4K
    val isHdr: Boolean = false, // optional
    val watchedPosition: Long = 0L
): Parcelable