// viewmodel/VideoListViewModel.kt
package com.ahmadzafartech.nextgenplayer.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmadzafartech.nextgenplayer.data.VideoRepository
import com.ahmadzafartech.nextgenplayer.domain.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoListViewModel(private val repo: VideoRepository = VideoRepository()) : ViewModel() {
    val videos = mutableStateListOf<Video>()
    fun loadVideos(context: Context, folderName: String) {
        videos.clear()
        val list = repo.getVideosInFolder(context, folderName)

        // Add videos immediately without thumbnails or duration
        videos.addAll(list)

        // Generate thumbnails and duration in background
        viewModelScope.launch(Dispatchers.IO) {
            list.forEach { video ->
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, video.uri)

                    // Thumbnail
                    val bmp = retriever.getFrameAtTime(1_000_000) // 1 second

                    // Duration
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val dur = durStr?.toLongOrNull()

                    retriever.release()

                    // Update on Main
                    viewModelScope.launch(Dispatchers.Main) {
                        val idx = videos.indexOfFirst { it.uri == video.uri }
                        if (idx != -1) {
                            videos[idx] = video.copy(
                                thumbnail = bmp,
                                duration = dur
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore errors
                }
            }
        }
    }



    fun generateThumbnail(context: Context, uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(1_000_000) // 1 second
        } catch (e: Exception) {
            null
        }
    }
}
