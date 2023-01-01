package com.hybcode.maplayer.musiclibrary

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide.init
import com.hybcode.maplayer.common.data.contentresolver.ContentResolverHelper
import com.hybcode.maplayer.common.data.repository.SongRepository
import com.hybcode.maplayer.common.domain.model.Song
import kotlinx.coroutines.launch
import javax.inject.Inject

class MusicLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val _allSongs: MutableLiveData<List<Song>> = MutableLiveData<List<Song>>()
    val allSongs: LiveData<List<Song>> get() = _allSongs
    private val repository: SongRepository
    private val cr: ContentResolverHelper
    init {
        cr = ContentResolverHelper(application)
        repository = SongRepository(cr)
        getSongList()
    }

    fun getSongList() = viewModelScope.launch {
        _allSongs.value = repository.getSongData()
    }
}