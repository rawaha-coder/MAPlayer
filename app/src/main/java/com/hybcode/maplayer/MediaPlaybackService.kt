package com.hybcode.maplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.Toast
import androidx.media.AudioManagerCompat.AUDIOFOCUS_GAIN
import androidx.media.MediaBrowserServiceCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hybcode.maplayer.data.model.Song
import java.io.IOException

class MediaPlaybackService : MediaBrowserServiceCompat(),  OnErrorListener {

    private val channelID = "music"
    private var currentlyPlayingSong: Song? = null
    private val handler = Handler(Looper.getMainLooper())
    private val logTag = "AudioPlayer"
    private var mMediaPlayer: MediaPlayer? = null
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var mMediaSessionCompat: MediaSessionCompat

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT -> mMediaSessionCallback.onPause()
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mMediaPlayer?.isPlaying == true) mMediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AUDIOFOCUS_GAIN -> mMediaPlayer?.setVolume(1.0f, 1.0f)
        }
    }

    private val mMediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val key: KeyEvent? = mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            if (key != null && mMediaPlayer != null) {
                when (key.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (mMediaPlayer!!.isPlaying) onPause()
                        else onPlay()
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> onPlay()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> onPause()
                    KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> onSkipToPrevious()
                    KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> onSkipToNext()
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPlay() {
            super.onPlay()
            if (currentlyPlayingSong != null) {
                val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).run {
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setOnAudioFocusChangeListener(audioFocusChangeListener)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    })
                    build()
                }
                val result = audioManager.requestAudioFocus(audioFocusRequest)
                if (result == AUDIOFOCUS_REQUEST_GRANTED) {
                    startService(Intent(applicationContext, MediaBrowserService::class.java))
                    mMediaSessionCompat.isActive = true
                    showNotification(true)
                    try {
                        mMediaPlayer!!.start()
                        val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                        val playbackDuration = mMediaPlayer!!.duration
                        val bundle = Bundle()
                        bundle.putInt("duration", playbackDuration)
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1f, bundle)
                        mMediaPlayer!!.setOnCompletionListener {
                            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 0f, null)
                        }
                    } catch (e: IllegalStateException) {
                        onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
                    } catch (e: NullPointerException) {
                        onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
                    }
                }
            }
        }

        override fun onPause() {
            super.onPause()
            if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.pause()
                val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                val playbackDuration = mMediaPlayer!!.duration
                val bundle = Bundle()
                bundle.putInt("duration", playbackDuration)
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition, 0f, bundle)
                showNotification(false)
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 0f, null)
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            if (mMediaPlayer != null && mMediaPlayer!!.currentPosition > 5000) onSeekTo(0)
            else setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 0f, null)
        }

        override fun onStop() {
            super.onStop()
            if (mMediaPlayer != null) {
                mMediaPlayer!!.stop()
                mMediaPlayer!!.release()
                mMediaPlayer = null
                currentlyPlayingSong = null
                stopForeground(true)
                try {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)} catch (ignore: UninitializedPropertyAccessException){ }
            }
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f, null)
            stopSelf()
        }

        override fun onSeekTo(position: Long) {
            super.onSeekTo(position)
            mMediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    it.seekTo(position.toInt())
                    it.start()
                    val playbackPosition = it.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1f, null)
                } else {
                    it.seekTo(position.toInt())
                    val playbackPosition = it.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition, 0f, null)
                }
            }
        }

        override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
            super.onPrepareFromUri(uri, extras)
            val bundle = extras!!.getString("song")
            val type = object : TypeToken<Song>() {}.type
            currentlyPlayingSong = Gson().fromJson(bundle, type)
            setCurrentMetadata()
            if (mMediaPlayer != null) mMediaPlayer!!.release()
            try {
                mMediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(application, Uri.parse(currentlyPlayingSong!!.uri))
                    setOnErrorListener(this@MediaPlaybackService)
                    prepare()
                    showNotification(false)
                }
            } catch (e: IOException) {
                onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            } catch (e: IllegalStateException) {
                onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            } catch (e: IllegalArgumentException) {
                onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_MALFORMED)
            }
        }
    }

    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) mMediaSessionCallback.onPause()
        }
    }

    override fun onCreate() {
        super.onCreate()
        mMediaSessionCompat = MediaSessionCompat(baseContext, logTag).apply {
            setCallback(mMediaSessionCallback)
            setSessionToken(sessionToken)
        }
        val filter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(mNoisyReceiver, filter)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            BrowserRoot(getString(R.string.app_name), null)
        } else null
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    override fun onError(mediaPlayer: MediaPlayer?, what: Int, extra: Int): Boolean {
        mMediaPlayer?.reset()
        mMediaPlayer = null
        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f, null)
        currentlyPlayingSong = null
        stopForeground(true)
        Toast.makeText(application, getString(R.string.error), Toast.LENGTH_LONG).show()
        return true
    }

    private fun setMediaPlaybackState(state: Int, position: Long, playbackSpeed: Float, bundle: Bundle?) {
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, playbackSpeed)
            .setExtras(bundle)
            .build()
        mMediaSessionCompat.setPlaybackState(playbackState)
    }
}