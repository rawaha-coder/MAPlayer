package com.hybcode.maplayer.common.domain.exoplayer

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.hybcode.maplayer.common.domain.services.Constants
import com.hybcode.maplayer.common.domain.services.ExtendMediaBrowserService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.mutableStateOf
import com.hybcode.maplayer.common.domain.model.Song
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


class MediaPlayerServiceConnection @Inject constructor(
    @ApplicationContext context: Context
) {

    private val _playBackState: MutableStateFlow<PlaybackStateCompat?> =
        MutableStateFlow(null)
    val playBackState: StateFlow<PlaybackStateCompat?>
        get() = _playBackState

    private val _isConnected: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val isConnected: StateFlow<Boolean>
        get() = _isConnected

    val currentPlayingSong = mutableStateOf<Song?>(null)

    lateinit var mediaControllerCompat: MediaControllerCompat

    private val mediaBrowserConnectionCallBack = MediaBrowserConnectionCallBack(context)

    private val mediaBrowser = MediaBrowserCompat(context,
        ComponentName(context, ExtendMediaBrowserService::class.java),
        mediaBrowserConnectionCallBack,
        null).apply {
        connect()
    }

    private var songList = listOf<Song>()

    val rootMediaId: String
        get() = mediaBrowser.root

    val transportControl: MediaControllerCompat.TransportControls
        get() = mediaControllerCompat.transportControls


    fun playMedia(songs:List<Song>){
        songList = songs
        mediaBrowser.sendCustomAction(Constants.START_MEDIA_PLAY_ACTION,null,null)
    }

    fun fastForward(seconds:Int = 10){
        playBackState.value?.currentPosition?.let {
            transportControl.seekTo(it + seconds * 1000)
        }
    }

    fun rewind(seconds:Int = 10){
        playBackState.value?.currentPosition?.let {
            transportControl.seekTo(it - seconds * 1000)
        }
    }

    fun skipToNext(){
        transportControl.skipToNext()
    }

    fun skipToPreview(){
        transportControl.skipToPrevious()
    }

    fun subscribe(
        parentId:String,
        callBack: MediaBrowserCompat.SubscriptionCallback
    ){
        mediaBrowser.subscribe(parentId,callBack)
    }

    fun unSubscribe(
        parentId:String,
        callBack:MediaBrowserCompat.SubscriptionCallback
    ){
        mediaBrowser.unsubscribe(parentId,callBack)
    }

    fun refreshMediaBrowserChildren(){
        mediaBrowser.sendCustomAction(
            Constants.REFRESH_MEDIA_PLAY_ACTION,
            null,
            null
        )
    }

    private inner class MediaBrowserConnectionCallBack(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            _isConnected.value = true
            mediaControllerCompat = MediaControllerCompat(
                context,
                mediaBrowser.sessionToken
            ).apply {
                registerCallback(MediaControllerCallBack())
            }
        }

        override fun onConnectionSuspended() {
            _isConnected.value = false
        }

        override fun onConnectionFailed() {
            _isConnected.value = false
        }
    }


    private inner class MediaControllerCallBack : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            _playBackState.value = state
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            currentPlayingSong.value = metadata?.let { data ->
                songList.find {
                    it.songID.toString() == data.description.mediaId
                }
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaBrowserConnectionCallBack.onConnectionSuspended()
        }
    }
}