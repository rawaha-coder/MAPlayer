package com.hybcode.maplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.Menu
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.hybcode.maplayer.data.model.Song
import com.hybcode.maplayer.databinding.ActivityMainBinding
import com.hybcode.maplayer.playback.PlaybackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val channelID = "music"
    private var completeLibrary = listOf<Song>()
    private var currentlyPlayingQueueID = 0
    private var playbackState = STATE_STOPPED
    private val playbackViewModel: PlaybackViewModel by viewModels()
    var playQueue = mutableListOf<Pair<Int, Song>>()
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var sharedPreferences: SharedPreferences

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            mediaBrowser.sessionToken.also { token ->
                val mediaControllerCompat = MediaControllerCompat(this@MainActivity, token)
                mediaControllerCompat.registerCallback(controllerCallback)
                MediaControllerCompat.setMediaController(this@MainActivity, mediaControllerCompat)
            }
            MediaControllerCompat
                .getMediaController(this@MainActivity)
                .registerCallback(controllerCallback)
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            if (state == null) return
            when (state.state) {
                STATE_PLAYING -> {
                    playbackState = state.state
                    val playbackPosition = state.position.toInt()
                    if (state.extras != null) {
                        val playbackDuration = state.extras!!.getInt("duration")
                        playbackViewModel.currentPlaybackDuration.value = playbackDuration
                    }
                    playbackViewModel.currentPlaybackPosition.value = playbackPosition
                    playbackViewModel.isPlaying.value = true
                }
                STATE_PAUSED -> {
                playbackState = state.state
                val playbackPosition = state.position.toInt()
                if (state.extras != null) {
                    val playbackDuration = state.extras!!.getInt("duration")
                    playbackViewModel.currentPlaybackDuration.value = playbackDuration
                }
                playbackViewModel.currentPlaybackPosition.value = playbackPosition
                playbackViewModel.isPlaying.value = false
            }
                STATE_STOPPED -> {
                playbackState = state.state
                playbackViewModel.isPlaying.value = false
                playbackViewModel.currentPlayQueue.value = mutableListOf()
                playbackViewModel.currentlyPlayingQueueID.value = 0
                playbackViewModel.currentlyPlayingSong.value = null
                playbackViewModel.currentPlaybackDuration.value = 0
                playbackViewModel.currentPlaybackPosition.value = 0
            }
                STATE_SKIPPING_TO_NEXT -> {
                val repeatSetting = sharedPreferences.getInt("repeat", REPEAT_MODE_NONE)
                when {
                    repeatSetting == REPEAT_MODE_ONE -> {}
                    playQueue.isNotEmpty() && playQueue[playQueue.size - 1].first != currentlyPlayingQueueID -> {
                        val index = playQueue.indexOfFirst {
                            it.first == currentlyPlayingQueueID
                        }
                        currentlyPlayingQueueID = playQueue[index + 1].first
                    }
                    repeatSetting == REPEAT_MODE_ALL -> currentlyPlayingQueueID = playQueue[0].first
                    else -> {
                        mediaController.transportControls.stop()
                        return
                    }
                }
                lifecycleScope.launch {
                            updateCurrentlyPlaying()
                            if (playbackState == STATE_PLAYING) play()
                        }
            }
                STATE_SKIPPING_TO_PREVIOUS -> {
                if (playQueue.isNotEmpty() && currentlyPlayingQueueID != playQueue[0].first) {
                    val index = playQueue.indexOfFirst {
                        it.first == currentlyPlayingQueueID
                    }
                    currentlyPlayingQueueID = playQueue[index - 1].first
                    lifecycleScope.launch {
                        updateCurrentlyPlaying()
                        if (playbackState == STATE_PLAYING) play()
                    }
                }
            }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallbacks,
            intent.extras
        )
        mediaBrowser.connect()
        createChannel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaControllerCompat.getMediaController(this)?.apply {
            transportControls.stop()
            unregisterCallback(controllerCallback)
        }
        mediaBrowser.disconnect()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            channelID, "Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "All app notifications"
            setSound(null, null)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun playNewSongs(playlist: List<Song>, startSong: Int?, shuffle: Boolean) = lifecycleScope.launch(
        Dispatchers.Main) {
        if (playlist.isNotEmpty()) {
            playQueue = mutableListOf()
            for ((i, s) in playlist.withIndex()) {
                val queueItem = Pair(i, s)
                playQueue.add(queueItem)
            }
            if (shuffle) playQueue.shuffle()
            currentlyPlayingQueueID = if (shuffle) playQueue[0].first
            else startSong ?: 0
            sharedPreferences.edit()
                .putBoolean("shuffle", shuffle)
                .apply()
            updateCurrentlyPlaying()
            play()
        }
    }

    private suspend fun updateCurrentlyPlaying() {
        val index = playQueue.indexOfFirst {
            it.first == currentlyPlayingQueueID
        }
        val currentQueueItem = if (playQueue.isNotEmpty() && index != -1) playQueue[index]
        else return
        withContext(Dispatchers.IO) {
            playbackViewModel.currentlyPlayingSong.postValue(currentQueueItem.second)
            playbackViewModel.currentPlayQueue.postValue(playQueue)
            playbackViewModel.currentlyPlayingQueueID.postValue(currentlyPlayingQueueID)
            val bundle = Bundle()
            val gPretty = GsonBuilder().setPrettyPrinting().create().toJson(currentQueueItem.second)
            bundle.putString("song", gPretty)
            mediaController.transportControls.prepareFromUri(Uri.parse(currentQueueItem.second.uri), bundle)
        }
    }

    private fun play() = mediaController.transportControls.play()

}