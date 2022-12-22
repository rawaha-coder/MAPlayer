package com.hybcode.maplayer.playback

import android.content.SharedPreferences
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.hybcode.maplayer.common.domain.model.Song
import kotlinx.coroutines.launch

class PlaybackViewModel : ViewModel() {
    var currentPlayQueue = MutableLiveData<List<Pair<Int, Song>>>()
    var currentPlaybackDuration = MutableLiveData<Int>()
    var currentPlaybackPosition = MutableLiveData<Int>()
    var currentlyPlayingQueueID = MutableLiveData<Int>()
    var currentlyPlayingSong = MutableLiveData<Song?>()
    var isPlaying = MutableLiveData<Boolean>()

    fun playbackPlay(state: PlaybackStateCompat){
        val playbackPosition = state.position.toInt()
        if (state.extras != null) {
            val playbackDuration = state.extras!!.getInt("duration")
            currentPlaybackDuration.value = playbackDuration
        }
        currentPlaybackPosition.value = playbackPosition
        isPlaying.value = true
    }

    fun playbackPause(state: PlaybackStateCompat){
        val playbackPosition = state.position.toInt()
        if (state.extras != null) {
            val playbackDuration = state.extras!!.getInt("duration")
            currentPlaybackDuration.value = playbackDuration
        }
        currentPlaybackPosition.value = playbackPosition
        isPlaying.value = false
    }

    fun playbackStop(){
        isPlaying.value = false
        currentPlayQueue.value = mutableListOf()
        currentlyPlayingQueueID.value = 0
        currentlyPlayingSong.value = null
        currentPlaybackDuration.value = 0
        currentPlaybackPosition.value = 0
    }
}