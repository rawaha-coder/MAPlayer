package com.hybcode.maplayer.common.data

import androidx.lifecycle.LiveData
import com.hybcode.maplayer.common.domain.repository.MusicRepository
import com.hybcode.maplayer.common.domain.model.Song

class MusicRepositoryImp(private val musicDao: MusicDao): MusicRepository {

    override val allSongs: LiveData<List<Song>> = musicDao.getAllSongs()

    override suspend fun insertSong(song: Song) {
        musicDao.insert(song)
    }

    override suspend fun deleteSong(song: Song) {
        musicDao.delete(song)
    }

    override suspend fun updateSong(song: Song){
        musicDao.update(song)
    }
}