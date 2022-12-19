package com.hybcode.maplayer.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.hybcode.maplayer.common.data.MusicRepositoryImp
import com.hybcode.maplayer.common.data.db.MusicDatabase
import com.hybcode.maplayer.common.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: MusicRepository
    val allSongs: LiveData<List<Song>>

    init {
        val musicDao = MusicDatabase.getDatabase(application).musicDao()
        repository = MusicRepositoryImp(musicDao)
        allSongs = repository.allSongs
    }

    fun deleteSong(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteSong(song)
    }

    fun insertSong(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSong(song)
    }

    fun updateMusicInfo(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateSong(song)
    }
}