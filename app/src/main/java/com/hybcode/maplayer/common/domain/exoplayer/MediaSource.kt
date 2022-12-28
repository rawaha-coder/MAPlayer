package com.hybcode.maplayer.common.domain.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.hybcode.maplayer.common.data.repository.SongRepository
import javax.inject.Inject

class MediaSource
@Inject constructor(private val repository: SongRepository) {

    private val onReadyListeners: MutableList<OnReadyListener> = mutableListOf()

    var songMediaMetaData: List<MediaMetadataCompat> = emptyList()

    private var state: MediaSourceState = MediaSourceState.STATE_CREATED
        set(value) {
            if (
                value == MediaSourceState.STATE_CREATED
                || value == MediaSourceState.STATE_ERROR
            ) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener: OnReadyListener ->
                        listener.invoke(isReady)
                    }
                }
            } else {
                field = value
            }
        }


    suspend fun load() {
        state = MediaSourceState.STATE_INITIALIZING
        val data = repository.getSongData()
        songMediaMetaData = data.map { song ->
            MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    song.songID.toString()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                    song.track.toString()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    song.title
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    song.artist
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM,
                    song.album
                ).putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    song.duration.toLong()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                    song.uri
                ).putLong(
                    MediaMetadataCompat.METADATA_KEY_YEAR,
                    song.year.toLong()
                )
                .build()
        }
        state = MediaSourceState.STATE_INITIALIZED
    }

    fun asMediaSource(dataSource: CacheDataSource.Factory):
            ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()

        songMediaMetaData.forEach { mediaMetadataCompat ->
            val mediaItem = MediaItem.fromUri(
                mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
            )

            val mediaSource = ProgressiveMediaSource
                .Factory(dataSource)
                .createMediaSource(mediaItem)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asMediaItem() = songMediaMetaData.map { metaData ->
        val description = MediaDescriptionCompat.Builder()
            .setTitle(metaData.description.title)
            .setMediaId(metaData.description.mediaId)
            .setSubtitle(metaData.description.subtitle)
            .setMediaUri(metaData.description.mediaUri)
            .build()
        MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE)

    }.toMutableList()


    fun refresh() {
        onReadyListeners.clear()
        state = MediaSourceState.STATE_CREATED
    }

    fun whenReady(listener: OnReadyListener): Boolean {
        return if (
            state == MediaSourceState.STATE_CREATED
            || state == MediaSourceState.STATE_INITIALIZING
        ) {
            onReadyListeners += listener
            false
        } else {
            listener.invoke(isReady)
            true
        }
    }

    private val isReady: Boolean
        get() = state == MediaSourceState.STATE_INITIALIZED

}

enum class MediaSourceState {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR,

}
typealias OnReadyListener = (Boolean) -> Unit