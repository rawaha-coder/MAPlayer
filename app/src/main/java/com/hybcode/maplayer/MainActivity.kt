package com.hybcode.maplayer

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Size
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.gson.GsonBuilder
import com.hybcode.maplayer.common.presentation.MusicViewModel
import com.hybcode.maplayer.common.data.db.MusicDatabase
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.common.services.MediaPlaybackService
import com.hybcode.maplayer.common.utils.FileConstants.channelID
import com.hybcode.maplayer.databinding.ActivityMainBinding
import com.hybcode.maplayer.playback.PlaybackViewModel
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    //private val channelID = "music"
    private var completeLibrary = listOf<Song>()
    private var currentlyPlayingQueueID = 0
    private var playbackState = STATE_STOPPED
    private val playbackViewModel: PlaybackViewModel by viewModels()
    var playQueue = mutableListOf<Pair<Int, Song>>()
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var searchView: SearchView
    private var currentPlaybackPosition = 0
    private var currentPlaybackDuration = 0

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
                    playbackViewModel.playbackPlay(state)
                }
                STATE_PAUSED -> {
                    playbackState = state.state
                    playbackViewModel.playbackPause(state)
                }
                STATE_STOPPED -> {
                    playbackState = state.state
                    playbackViewModel.playbackStop()
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
                        repeatSetting == REPEAT_MODE_ALL -> currentlyPlayingQueueID =
                            playQueue[0].first
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
        setSupportActionBar(binding.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_queue, R.id.nav_library, R.id.nav_songs),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //initialise the sharedPreferences and mediaBrowser
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallbacks,
            intent.extras
        )
        mediaBrowser.connect()

        createChannel()

        val mOnNavigationItemSelectedListener =
            NavigationView.OnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_queue -> {
                        val action = MobileNavigationDirections.actionLibrary(0)
                        navController.navigate(action)
                    }
                    R.id.nav_songs -> {
                        val action = MobileNavigationDirections.actionLibrary(1)
                        navController.navigate(action)
                    }
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                true
            }
        binding.navView.setNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        binding.navView.itemIconTintList = null

        musicViewModel = ViewModelProvider(this)[MusicViewModel::class.java]

        playbackViewModel.currentPlayQueue.observe(this) { queue ->
            queue?.let {
                playQueue = queue.toMutableList()
            }
        }
        playbackViewModel.currentlyPlayingQueueID.observe(this) {
            currentlyPlayingQueueID = it
        }

        playbackViewModel.currentPlaybackPosition.observe(this) { position ->
            position?.let {
                currentPlaybackPosition = it
            }
        }
        playbackViewModel.currentPlaybackDuration.observe(this) { duration ->
            duration?.let {
                currentPlaybackDuration = it
            }
        }

        musicViewModel.allSongs.observe(this) { songs ->
            songs.let { completeLibrary = it.toMutableList() }
        }
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            libraryMaintenance()
        } else ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            libraryMaintenance()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun libraryMaintenance() = lifecycleScope.launch(Dispatchers.Main) {
        libraryRefresh()
        if (completeLibrary.isNotEmpty()) {
            val songsToDelete = checkLibrarySongsExistAsync().await()
            if (songsToDelete.isNotEmpty()) deleteSongs(songsToDelete)
        }
    }

    private fun checkLibrarySongsExistAsync(): Deferred<List<Song>> =
        lifecycleScope.async(Dispatchers.IO) {
            var songsToBeDeleted = mutableListOf<Song>()
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val libraryCursor = musicQueryAsync(projection).await()
            libraryCursor?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                songsToBeDeleted = completeLibrary.toMutableList()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val indexOfSong = songsToBeDeleted.indexOfFirst { song: Song ->
                        song.songID == id
                    }
                    if (indexOfSong != -1) songsToBeDeleted.removeAt(indexOfSong)
                }
            }
            return@async songsToBeDeleted
        }

    private suspend fun deleteSongs(songs: List<Song>) {
        for (s in songs) {
// Remove all instances of the song from the play queue
            if (playQueue.isNotEmpty()) {
                do {
                    val index = playQueue.indexOfFirst {
                        it.second.songID == s.songID
                    }
                    if (index != -1) removeQueueItem(index)
                } while (index != -1)
            }
            musicViewModel.deleteSong(s)
        }
        tidyArtwork(songs)
    }

    private suspend fun tidyArtwork(songs: List<Song>) {
        val directory = ContextWrapper(application).getDir("albumArt", Context.MODE_PRIVATE)
        val musicDatabase = MusicDatabase.getDatabase(this)
        for (s in songs) {
            val artworkInUse = musicDatabase.musicDao().getAlbum(s.albumID)
            if (artworkInUse.isNullOrEmpty()) {
                val path = File(directory, s.albumID + ".jpg")
                if (path.exists()) path.delete()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val searchItem = menu.findItem(R.id.search)
        searchView = searchItem!!.actionView as SearchView
        searchView.setOnSearchClickListener {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_search)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        val id = findNavController(R.id.nav_controls_fragment).currentDestination?.id ?: 0
        if (id == R.id.nav_currently_playing) {
            findNavController(R.id.nav_controls_fragment).popBackStack()
            hideSystemBars(false)
        } else super.onBackPressed()
        if (!searchView.isIconified) {
            searchView.isIconified = true
            searchView.onActionViewCollapsed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        if (!searchView.isIconified) {
            searchView.isIconified = true
            searchView.onActionViewCollapsed()
        }
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
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun playNewSongs(playlist: List<Song>, startSong: Int?, shuffle: Boolean) =
        lifecycleScope.launch(
            Dispatchers.Main
        ) {
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
            mediaController.transportControls.prepareFromUri(
                Uri.parse(currentQueueItem.second.uri),
                bundle
            )
        }
    }

    private fun play() = mediaController.transportControls.play()

    fun insertArtwork(albumID: String?, view: ImageView) {
        var file: File? = null
        if (albumID != null) {
            val cw = ContextWrapper(this)
            val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
            file = File(directory, "$albumID.jpg")
        }
        Glide.with(this)
            .load(file ?: R.drawable.ic_launcher_foreground)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .signature(ObjectKey(file?.path + file?.lastModified()))
            .override(600, 600)
            .into(view)
    }

    fun showSongPopup(view: View, song: Song) {
        PopupMenu(this, view).apply {
            inflate(R.menu.song_options)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.play_next -> {
                        playNext(song)
                        true
                    }
                    R.id.edit_metadata -> {
                        val action = MobileNavigationDirections.actionEditSong(song)
                        findNavController(R.id.nav_host_fragment).navigate(action)
                        true
                    }
                    else -> super.onOptionsItemSelected(it)
                }
            }
            show()
        }
    }

    fun getArtworkFile(filename: String): File {
        val cw = ContextWrapper(application)
        val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
        return File(directory, "$filename.jpg")
    }

    fun saveImage(bitmap: Bitmap, path: File) {
        try {
            FileOutputStream(path).apply {
// Use the compress method on the BitMap object to write image to the OutputStream
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
                close()
            }
        } catch (ignore: Exception) {
        }
    }

    fun updateSongInfo(song: Song) = lifecycleScope.launch(Dispatchers.Default) {
        musicViewModel.updateMusicInfo(song)
// See if the play queue needs to be updated
        if (playQueue.isNotEmpty()) {
            val newQueue = playQueue
            fun findIndex(): Int {
                return newQueue.indexOfFirst {
                    it.second.songID == song.songID
                }
            }
            // HashMap key = queue index, value = queue item ID
            val queueIndexQueueIDMap = HashMap<Int, Int>()
            do {
                val index = findIndex()
                if (index != -1) {
                    queueIndexQueueIDMap[index] = playQueue[index].first
                    newQueue.removeAt(index)
                }
            } while (index != -1)
// Add the affected queue items (with updated metadata) back to the play queue
            for ((index, queueID) in queueIndexQueueIDMap) {
                val queueItem = Pair(queueID, song)
                newQueue.add(index, queueItem)
            }
            playbackViewModel.currentPlayQueue.postValue(newQueue)
        }
    }

    fun skipToQueueItem(position: Int) {
        currentlyPlayingQueueID = playQueue[position].first
        lifecycleScope.launch {
            updateCurrentlyPlaying()
            play()
        }
    }

    fun removeQueueItem(index: Int) {
        if (playQueue.isNotEmpty() && index != -1) {
// Check if the currently playing song is being removed from the play queue
            val currentlyPlayingSongRemoved = playQueue[index].first == currentlyPlayingQueueID
            playQueue.removeAt(index)
            if (currentlyPlayingSongRemoved) {
                currentlyPlayingQueueID = when {
                    playQueue.isEmpty() -> {
                        val mediaController = MediaControllerCompat.getMediaController(this)
                        mediaController.transportControls.stop()
                        return
                    }
                    playQueue.size == index -> playQueue[0].first
                    else -> playQueue[index].first
                }
                lifecycleScope.launch {
                    updateCurrentlyPlaying()
                    if (playbackState == STATE_PLAYING) play()
                }
            }
            playbackViewModel.currentPlayQueue.value = playQueue
        }
    }

    private fun playNext(song: Song) {
        val sortedQueue = playQueue.sortedByDescending {
            it.first
        }
        val highestQueueID = if (sortedQueue.isNotEmpty()) sortedQueue[0].first
        else -1
        val queueItem = Pair(highestQueueID + 1, song)
        val index = playQueue.indexOfFirst {
            it.first == currentlyPlayingQueueID
        }
        playQueue.add(index + 1, queueItem)
        playbackViewModel.currentPlayQueue.value = playQueue
        Toast.makeText(this, getString(R.string.added_to_queue, song.title), Toast.LENGTH_SHORT)
            .show()
    }

    fun hideKeyboard(activity: Activity) {
        val inputManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = activity.currentFocus
        if (currentFocusedView != null) inputManager.hideSoftInputFromWindow(
            currentFocusedView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    fun playPauseControl() {
        when (playbackState) {
            STATE_PAUSED -> play()
            STATE_PLAYING -> mediaController.transportControls.pause()
            else -> {
// Play the first song in the library if the play queue is currently empty
                if (playQueue.isNullOrEmpty()) playNewSongs(completeLibrary, 0, false)
                else {
// It's possible a queue has been built without ever pressing play
                    lifecycleScope.launch {
                        updateCurrentlyPlaying()
                        play()
                    }
                }
            }
        }
    }

    fun skipBack() = mediaController.transportControls.skipToPrevious()
    fun skipForward() = mediaController.transportControls.skipToNext()

    fun fastRewind() {
        val pos = currentPlaybackPosition - 5000
        if (pos < 0) skipBack()
        else mediaController.transportControls.seekTo(pos.toLong())
    }

    fun fastForward() {
        val pos = currentPlaybackPosition + 5000
        if (pos > currentPlaybackDuration) skipForward()
        else mediaController.transportControls.seekTo(pos.toLong())
    }

    fun hideSystemBars(hide: Boolean) {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        if (hide) {
// Configure the behavior of the hidden system bars
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
// Hide both the status bar and the navigation bar
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
// Hide the toolbar to prevent the SearchView keyboard inadvertently popping up
            binding.toolbar.visibility = View.GONE
        } else {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.visibility = View.VISIBLE
        }
    }

    // Returns true if the play queue has been shuffled, false if unshuffled
    fun shuffleCurrentPlayQueue(): Boolean {
        val isShuffled = sharedPreferences.getBoolean("shuffle", false)
        if (playQueue.isNotEmpty()) {
            if (isShuffled) {
                playQueue.sortBy { it.first }
                Toast.makeText(applicationContext, "Play queue unshuffled", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val currentQueueItem = playQueue.find {
                    it.first == currentlyPlayingQueueID
                }
                if (currentQueueItem != null) {
                    playQueue.remove(currentQueueItem)
                    playQueue.shuffle()
                    playQueue.add(0, currentQueueItem)
                    Toast.makeText(applicationContext, "Play queue shuffled", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            playbackViewModel.currentPlayQueue.value = playQueue
        }
        sharedPreferences.edit().apply {
            putBoolean("shuffle", !isShuffled)
            apply()
        }
        return !isShuffled
    }

    fun setRepeatMode(repeatMode: Int): SharedPreferences.Editor = sharedPreferences
        .edit()
        .apply {
            putInt("repeat", repeatMode)
            apply()
        }

    fun seekTo(position: Int) = mediaController.transportControls.seekTo(position.toLong())

    private fun musicQueryAsync(projection: Array<String>): Deferred<Cursor?> =
        lifecycleScope.async(Dispatchers.IO) {
            val selection = MediaStore.Audio.Media.IS_MUSIC
            val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
            return@async application.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )
        }

    private suspend fun libraryRefresh() = lifecycleScope.launch(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        )
        val libraryCursor = musicQueryAsync(projection).await()
        libraryCursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
// Check the song has not been added to library. Will return -1 if not in library
                val indexOfSong = completeLibrary.indexOfFirst { song: Song ->
                    song.songID == id
                }
                if (indexOfSong == -1) {
                    var trackString = cursor.getString(trackColumn) ?: "1001"
// We need the track value in the format 1xxx, where the first digit is the disc number
                    val track = try {
                        when (trackString.length) {
                            4 -> trackString.toInt()
                            in 1..3 -> {
                                val numberNeeded = 4 - trackString.length
                                trackString = when (numberNeeded) {
                                    1 -> "1$trackString"
                                    2 -> "10$trackString"
                                    else -> "100$trackString"
                                }
                                trackString.toInt()
                            }
                            else -> 1001
                        }
                    } catch (e: NumberFormatException) {
// If the track format is incorrect (e.g. "12/23") then simply set track to 1001
                        1001
                    }
                    val title = cursor.getString(titleColumn) ?: "Unknown song"
                    val artist = cursor.getString(artistColumn) ?: "Unknown artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown album"
                    val year = cursor.getString(yearColumn) ?: "2000"
                    val albumID = cursor.getString(albumIDColumn) ?: "unknown_album_ID"
                    val uri =
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
// URI needs to be converted to a string for storage
                    val songUri = uri.toString()
                    val file = getArtworkFile(albumID)
// If artwork is not saved then try and extract artwork from the audio file
                    if (!file.exists()) {
                        val albumArt: Bitmap? = try {
                            application.contentResolver.loadThumbnail(uri, Size(640, 640), null)
                        } catch (e: FileNotFoundException) {
                            null
                        }
                        if (albumArt != null) saveImage(albumArt, file)
                    }
                    val song = Song(id, track, title, artist, album, albumID, songUri, year)
                    musicViewModel.insertSong(song)
                }
            }
        }
    }

}