package com.hybcode.maplayer.common.data.contentresolver

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.WorkerThread
import com.hybcode.maplayer.common.domain.model.Song
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class ContentResolverHelper(private val application: Application) {

    private val TAG = "ContentResolverHelper"

    @WorkerThread
    fun getSongData(): List<Song>{
        return getCursorData()
    }

    private fun getCursorData(): MutableList<Song>{
        val songList = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        )

        val musicLibraryCursor = musicQuery(projection, application)
        musicLibraryCursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            cursor.apply {
                if (count == 0){
                    Log.e(TAG, "getCursorData, Cursor is empty" )
                }else{
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val indexOfSong = songList.indexOfFirst { song: Song ->
                            song.songID == id
                        }
                        if (indexOfSong == -1) {
                            var trackString = cursor.getString(trackColumn) ?: "1001"
                            val track = try {
                                when (trackString.length) {
                                    4 -> trackString.toInt()
                                    in 1..3 -> {
                                        val numberNeeded = 4 - trackString.length
                                        trackString = when (numberNeeded) {
                                            1 -> "1$trackString"
                                            2 -> "10$trackString"
                                            else -> "100$trackString"
                                        }
                                        trackString.toInt()
                                    }
                                    else -> 1001
                                }
                            } catch (e: NumberFormatException) {
                                1001
                            }
                            val title = cursor.getString(titleColumn) ?: "Unknown song"
                            val artist = cursor.getString(artistColumn) ?: "Unknown artist"
                            val album = cursor.getString(albumColumn) ?: "Unknown album"
                            val year = cursor.getString(yearColumn) ?: "2000"
                            val albumID = cursor.getString(albumIDColumn) ?: "unknown_album_ID"
                            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            val songUri = uri.toString()
                            val file = getArtworkFile(albumID, application)
                            if (!file.exists()) {
                                val albumArt: Bitmap? = try {
                                    application.contentResolver.loadThumbnail(uri, Size(640, 640), null)
                                } catch (e: FileNotFoundException) {
                                    null
                                }
                                if (albumArt != null) saveImage(albumArt, file)
                            }
                            songList += Song(id, track, title, artist, album, albumID, songUri, year)
                        }
                    }
                }
            }
        }
        return songList
    }

    private fun musicQuery(projection: Array<String>,
                           application: Application): Cursor? {
        val selection = MediaStore.Audio.Media.IS_MUSIC
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        return application.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }

    private fun getArtworkFile(filename: String, application: Application): File {
        val cw = ContextWrapper(application)
        val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
        return File(directory, "$filename.jpg")
    }

    private fun saveImage(bitmap: Bitmap, path: File) {
        try {
            FileOutputStream(path).apply {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
                close()
            }
        } catch (ignore: Exception) {
            Log.e(TAG, "saveImage error: $ignore" )
        }
    }
}