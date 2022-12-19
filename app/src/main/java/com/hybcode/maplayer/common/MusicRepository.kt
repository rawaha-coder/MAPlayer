package com.hybcode.maplayer.common

import androidx.lifecycle.LiveData
import com.hybcode.maplayer.common.data.db.MusicDao
import com.hybcode.maplayer.common.domain.model.Song

interface MusicRepository {

    val allSongs: LiveData<List<Song>>

    suspend fun insertSong(song: Song)
    
    suspend fun deleteSong(song: Song)

    suspend fun updateSong(song: Song)
}