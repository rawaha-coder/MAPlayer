package com.hybcode.maplayer.common.data.contentresolver

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Size
import com.hybcode.maplayer.common.data.db.MusicDatabase
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.common.domain.repository.MusicRepository
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class MusicLibrary(val application: Application,
                   private val musicRepository: MusicRepository) {

    private var completeLibrary = listOf<Song>()

    suspend fun getSongData(): List<Song>{
        var musicLibrary = listOf<Song>()
        observeSongsIntegrity()
        musicRepository.allSongs.value.let {
            if (it != null) {
                musicLibrary = it.toMutableList()
            }
        }
        return musicLibrary
    }

    private suspend fun observeSongsIntegrity(){
        musicRepository.allSongs.value.let {
                if (it != null) {
                    completeLibrary = it.toMutableList()
                }
            }
        musicLibraryMaintenance()
    }

    suspend fun musicLibraryMaintenance() {
        musicLibraryRefresh()
        if (completeLibrary.isNotEmpty()) {
            val songsToDelete = checkLibrarySongsExistAsync().await()
            if (songsToDelete.isNotEmpty()) deleteSongs(songsToDelete)
        }
    }

    private suspend fun musicLibraryRefresh() {

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        )
        val libraryCursor = musicQueryAsync(projection).await()
        libraryCursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
// Check the song has not been added to library. Will return -1 if not in library
                val indexOfSong = completeLibrary.indexOfFirst { song: Song ->
                    song.songID == id
                }
                if (indexOfSong == -1) {
                    var trackString = cursor.getString(trackColumn) ?: "1001"
// We need the track value in the format 1xxx, where the first digit is the disc number
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
// If the track format is incorrect (e.g. "12/23") then simply set track to 1001
                        1001
                    }
                    val title = cursor.getString(titleColumn) ?: "Unknown song"
                    val artist = cursor.getString(artistColumn) ?: "Unknown artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown album"
                    val year = cursor.getString(yearColumn) ?: "2000"
                    val albumID = cursor.getString(albumIDColumn) ?: "unknown_album_ID"
                    val uri =
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
// URI needs to be converted to a string for storage
                    val songUri = uri.toString()
                    val file = getArtworkFile(albumID)
// If artwork is not saved then try and extract artwork from the audio file
                    if (!file.exists()) {
                        val albumArt: Bitmap? = try {
                            application.contentResolver.loadThumbnail(uri, Size(640, 640), null)
                        } catch (e: FileNotFoundException) {
                            null
                        }
                        if (albumArt != null) saveImage(albumArt, file)
                    }
                    val song = Song(id, track, title, artist, album, albumID, songUri, year)
                    musicRepository.insertSong(song)
                }
            }
        }
    }

    private suspend fun musicQueryAsync(projection: Array<String>): Deferred<Cursor?> = coroutineScope{
        async(Dispatchers.IO) {
            val selection = MediaStore.Audio.Media.IS_MUSIC
            val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
            return@async application.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )
        }}

    private fun getArtworkFile(filename: String): File {
        val cw = ContextWrapper(application)
        val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
        return File(directory, "$filename.jpg")
    }

    private fun saveImage(bitmap: Bitmap, path: File) {
        try {
            FileOutputStream(path).apply {
// Use the compress method on the BitMap object to write image to the OutputStream
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
                close()
            }
        } catch (ignore: Exception) {
        }
    }

    private suspend fun checkLibrarySongsExistAsync(): Deferred<List<Song>> = coroutineScope{
        async(Dispatchers.IO) {
            var songsToBeDeleted = mutableListOf<Song>()
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val libraryCursor = musicQueryAsync(projection).await()
            libraryCursor?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                songsToBeDeleted = completeLibrary.toMutableList()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val indexOfSong = songsToBeDeleted.indexOfFirst { song: Song ->
                        song.songID == id
                    }
                    if (indexOfSong != -1) songsToBeDeleted.removeAt(indexOfSong)
                }
            }
            return@async songsToBeDeleted
        }}

    private suspend fun deleteSongs(songs: List<Song>) {
        for (s in songs) {
            musicRepository.deleteSong(s)
        }
        tidyArtwork(songs)
    }

    private suspend fun tidyArtwork(songs: List<Song>) {
        val directory = ContextWrapper(application).getDir("albumArt", Context.MODE_PRIVATE)
        val musicDatabase = MusicDatabase.getDatabase(application)
        for (s in songs) {
            val artworkInUse = musicDatabase.musicDao().getAlbum(s.albumID)
            if (artworkInUse.isNullOrEmpty()) {
                val path = File(directory, s.albumID + ".jpg")
                if (path.exists()) path.delete()
            }
        }
    }
}