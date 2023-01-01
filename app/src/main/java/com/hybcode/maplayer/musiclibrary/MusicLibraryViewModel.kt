package com.hybcode.maplayer.musiclibrary


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hybcode.maplayer.common.data.repository.SongRepository
import com.hybcode.maplayer.common.domain.exoplayer.MediaPlayerServiceConnection
import com.hybcode.maplayer.common.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicLibraryViewModel @Inject constructor (
    private val repository: SongRepository,
    serviceConnection: MediaPlayerServiceConnection
) : ViewModel() {
    private val _allSongs: MutableLiveData<List<Song>> = MutableLiveData<List<Song>>()
    val allSongs: LiveData<List<Song>> get() = _allSongs

    init {
        viewModelScope.launch {
            _allSongs.value = repository.getSongData()
        }
    }


}