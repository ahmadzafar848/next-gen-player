package com.ahmadzafartech.nextgenplayer.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ahmadzafartech.nextgenplayer.domain.Video
import java.io.File




class VideoRepository {

    fun getFolders(context: Context): Map<String, List<Video>> {
        val folderMap = mutableMapOf<String, MutableList<Video>>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataIndex)
                val title = cursor.getString(nameIndex)
                val folderName = File(path).parentFile?.name ?: "Unknown"
                val video = Video(uri = Uri.fromFile(File(path)), title = title)

                if (!folderMap.containsKey(folderName)) {
                    folderMap[folderName] = mutableListOf()
                }
                folderMap[folderName]?.add(video)
            }
        }
        return folderMap
    }
    // ✅ Add this function
    fun getVideosInFolder(context: Context, folderName: String): List<Video> {
        return getFolders(context)[folderName] ?: emptyList()
    }
}



