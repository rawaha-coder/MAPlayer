package com.hybcode.maplayer.common.presentation

import android.support.v4.media.session.PlaybackStateCompat.*
import android.support.v4.media.MediaBrowserCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hybcode.maplayer.common.data.repository.SongRepository
import com.hybcode.maplayer.common.domain.exoplayer.MediaPlayerServiceConnection
import com.hybcode.maplayer.common.domain.exoplayer.currentPosition
import com.hybcode.maplayer.common.domain.exoplayer.isPlaying
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.common.domain.services.Constants
import com.hybcode.maplayer.common.domain.services.ExtendMediaBrowserService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongViewModel @Inject constructor(
    private val repository: SongRepository,
    serviceConnection: MediaPlayerServiceConnection
) : ViewModel() {

    var currentlyPlayingSong = MutableLiveData<Song?>()
    var isPlaying = MutableLiveData<Boolean>()
    var currentPlaybackPosition = MutableLiveData<Int>()
    var currentPlaybackDuration = MutableLiveData<Int>()
    private var playState = STATE_STOPPED

    //new val
    var songList = mutableStateListOf<Song>()
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
            songList += repository.getSongData() //getAndFormatSongData()
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

    private suspend fun getAndFormatSongData(): List<Song> {
        return repository.getSongData().map {
            val title = it.title.substringBefore(".")
            val artist = if (it.artist.contains("<unknown>"))
                "Unknown Artist" else it.artist
            it.copy(
                title = title,
                artist = artist
            )
        }
    }

    fun playMedia(currentSong: Song) {
        serviceConnection.playMedia(songList)
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

    fun playPauseControl(){
        when (playState) {
            STATE_PAUSED -> serviceConnection.transportControl.play()
            STATE_PLAYING -> serviceConnection.transportControl.pause()
            else -> {Unit }
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

    override fun onCleared() {
        super.onCleared()
        serviceConnection.unSubscribe(
            Constants.MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {}
        )
        updatePosition = false
    }
}