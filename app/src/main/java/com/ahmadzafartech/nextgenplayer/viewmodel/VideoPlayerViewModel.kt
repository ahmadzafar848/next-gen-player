// viewmodel/VideoPlayerViewModel.kt
package com.ahmadzafartech.nextgenplayer.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ahmadzafartech.nextgenplayer.domain.Video



class VideoPlayerViewModel : ViewModel() {

    var exoPlayer: ExoPlayer? by mutableStateOf(null)
    var playlist by mutableStateOf<List<Video>>(emptyList())
    var currentIndex by mutableStateOf(0)
    var isPlaying by mutableStateOf(false)
    var currentVideo: Video?
        get() = playlist.getOrNull(currentIndex)
        private set(_) {}

    fun setPlaylist(context: Context, videos: List<Video>, startIndex: Int = 0) {
        if (videos.isEmpty()) return
        playlist = videos
        currentIndex = startIndex.coerceIn(0, videos.size - 1)

        exoPlayer?.release()
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItems(playlist.map { MediaItem.fromUri(it.uri) })
            prepare()
            seekTo(currentIndex, 0)
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentIndex = currentMediaItemIndex
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    this@VideoPlayerViewModel.isPlaying = isPlayingNow
                }
            })
        }
        isPlaying = true
    }

    fun playPause() {
        exoPlayer?.let {
            it.playWhenReady = !it.playWhenReady
            isPlaying = it.playWhenReady
        }
    }

    fun seekToPosition(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun seekBy(ms: Long) {
        exoPlayer?.let {
            val newPos = (it.currentPosition + ms).coerceIn(0, it.duration)
            it.seekTo(newPos)
        }
    }

    fun next() {
        if (currentIndex < playlist.size - 1) {
            currentIndex++
            exoPlayer?.seekTo(currentIndex, 0)
            exoPlayer?.playWhenReady = true
        }
    }

    fun previous() {
        if (currentIndex > 0) {
            currentIndex--
            exoPlayer?.seekTo(currentIndex, 0)
            exoPlayer?.playWhenReady = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}


