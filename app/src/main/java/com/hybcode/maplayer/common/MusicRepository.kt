package com.hybcode.maplayer.common

import androidx.lifecycle.LiveData
import com.hybcode.maplayer.common.data.db.MusicDao
import com.hybcode.maplayer.common.data.model.Song

class MusicRepository(private val musicDao: MusicDao) {

    val allSongs: LiveData<List<Song>> = musicDao.getAllSongs()

    suspend fun insertSong(song: Song) {
        musicDao.insert(song)
    }
    
    suspend fun deleteSong(song: Song) {
        musicDao.delete(song)
    }

    suspend fun updateSong(song: Song){
        musicDao.update(song)
    }
}