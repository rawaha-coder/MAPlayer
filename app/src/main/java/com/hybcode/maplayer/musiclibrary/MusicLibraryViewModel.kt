package com.hybcode.maplayer.musiclibrary


import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.hybcode.maplayer.common.data.repository.SongRepository
import com.hybcode.maplayer.common.domain.exoplayer.MediaPlayerServiceConnection
import com.hybcode.maplayer.common.domain.exoplayer.currentPosition
import com.hybcode.maplayer.common.domain.exoplayer.isPlaying
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.common.domain.services.Constants
import com.hybcode.maplayer.common.domain.services.ExtendMediaBrowserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MusicLibraryViewModel @Inject constructor (
    private val repository: SongRepository,
    serviceConnection: MediaPlayerServiceConnection
) : ViewModel() {
    private val _allSongs: MutableLiveData<List<Song>> = MutableLiveData<List<Song>>()
    val allSongs: LiveData<List<Song>> get() = _allSongs

    private val _currentlyPlayingSong: MutableLiveData<Song> = MutableLiveData<Song>()
    val currentlyPlayingSong: LiveData<Song> get() = _currentlyPlayingSong

    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> get()  = _isPlaying

    var currentPlaybackDuration = MutableLiveData<Int>()
    var currentPlaybackPosition = MutableLiveData<Int>()


    val currentPlayingSong = serviceConnection.currentPlayingSong
    private val isConnected = serviceConnection.isConnected
    lateinit var rootMediaId: String
    var currentPlayBackPosition by mutableStateOf(0L)
    private var updatePosition = true
    private val playbackState = serviceConnection.playBackState
    val isSongPlaying: Boolean
        get() = playbackState.value?.isPlaying == true

    private val subscriptionCallback = object
        : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            super.onChildrenLoaded(parentId, children)
        }
    }

    private val serviceConnection = serviceConnection.also {
        updatePlayBack()
    }

    val currentDuration:Long
        get() = ExtendMediaBrowserService.currentDuration

    var currentSongProgress = mutableStateOf(0f)

    init {
        viewModelScope.launch {
            _allSongs.value = repository.getSongData()
            isConnected.collect {
                if (it) {
                    rootMediaId = serviceConnection.rootMediaId
                    serviceConnection.playBackState.value?.apply {
                        currentPlayBackPosition = position
                    }
                    serviceConnection.subscribe(rootMediaId, subscriptionCallback)
                }
            }
        }
    }

    fun playMedia(currentSong: Song) {
        _currentlyPlayingSong.value = currentSong
        _isPlaying.value = true
        allSongs.value.let {
                if (it != null) {
                    serviceConnection.playMedia(it)
                    if (currentSong.songID == currentPlayingSong.value?.songID) {
                        if (isSongPlaying) {
                            serviceConnection.transportControl.pause()
                        } else {
                            serviceConnection.transportControl.play()
                        }
                    } else {
                        serviceConnection.transportControl
                            .playFromMediaId(
                                currentSong.songID.toString(),
                                null
                            )
                    }
                }
        }
    }

    private fun updatePlayBack() {
        viewModelScope.launch {
            val position = playbackState.value?.currentPosition ?: 0

            if (currentPlayBackPosition != position) {
                currentPlayBackPosition = position
            }

            if (currentDuration > 0) {
                currentSongProgress.value = (
                        currentPlayBackPosition.toFloat()
                                / currentDuration.toFloat() * 100f
                        )
            }

            delay(Constants.PLAYBACK_UPDATE_INTERVAL)
            if (updatePosition) {
                updatePlayBack()
            }
        }
    }

    fun stopPlayBack() {
        serviceConnection.transportControl.stop()
    }

    fun fastForward() {
        serviceConnection.fastForward()
    }

    fun rewind() {
        serviceConnection.rewind()
    }

    fun skipToNext() {
        serviceConnection.skipToNext()
    }

    fun skipToPreview(){
        serviceConnection.transportControl.skipToPrevious()
    }

    fun seekTo(value: Float) {
        serviceConnection.transportControl.seekTo(
            (currentDuration * value / 100f).toLong()
        )
    }

    override fun onCleared() {
        super.onCleared()
        serviceConnection.unSubscribe(
            Constants.MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {}
        )
        updatePosition = false
    }

}