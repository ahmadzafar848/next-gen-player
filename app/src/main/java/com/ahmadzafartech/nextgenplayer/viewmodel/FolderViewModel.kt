package com.ahmadzafartech.nextgenplayer.viewmodel

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmadzafartech.nextgenplayer.data.VideoRepository
import com.ahmadzafartech.nextgenplayer.domain.VideoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FolderViewModel : ViewModel() {
    var folders = mutableStateListOf<VideoFolder>()
        private set

    fun loadFolders(context: Context) {
        folders.clear()
        val folderMap = VideoRepository().getFolders(context)

        folderMap.forEach { (folderName, videos) ->
            val title = folderName.replaceFirstChar { it.uppercase() }

            // Initially add folder with no thumbnail
            val folder = VideoFolder(
                name = folderName,
                path = videos.firstOrNull()?.uri?.path ?: "",
                title = title,
                thumbnail = null,
                videoCount = videos.size
            )
            folders.add(folder)

            // Generate thumbnail asynchronously
            val firstVideoUri = videos.firstOrNull()?.uri
            if (firstVideoUri != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val bmp = try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, firstVideoUri)
                        retriever.getFrameAtTime(1_000_000)
                    } catch (e: Exception) {
                        null
                    }

                    bmp?.let { bitmap ->
                        val index = folders.indexOf(folder)
                        if (index != -1) {
                            folders[index] = folder.copy(thumbnail = bitmap)
                        }
                    }
                }
            }
        }
    }


}

