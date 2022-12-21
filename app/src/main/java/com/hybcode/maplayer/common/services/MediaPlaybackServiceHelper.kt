package com.hybcode.maplayer.common.services

import android.app.Application
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException


class MediaPlaybackServiceHelper {
    companion object {
        fun getMediaMetadata(
            song: Song?,
            application: Application,
            applicationContext: Context
        ): MediaMetadataCompat =
            MediaMetadataCompat.Builder().apply {
                putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    song?.title
                )
                putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    song?.artist
                )
                putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM,
                    song?.album
                )
                putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    getArtwork(song?.albumID, applicationContext) ?: BitmapFactory.decodeResource(
                        application.resources,
                        R.drawable.ic_launcher_foreground
                    )
                )
            }.build()

        private fun getArtwork(albumArtwork: String?, applicationContext: Context): Bitmap? {
            try {
                return BitmapFactory.Options().run {
                    inJustDecodeBounds = true
                    val cw = ContextWrapper(applicationContext)
                    val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
                    val f = File(directory, "$albumArtwork.jpg")
                    BitmapFactory.decodeStream(FileInputStream(f))
                    inSampleSize = calculateInSampleSize(this)
                    inJustDecodeBounds = false
                    BitmapFactory.decodeStream(FileInputStream(f))
                }
            } catch (ignore: FileNotFoundException) {

            }
            return null
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
            val reqWidth = 100
            val reqHeight = 100
            val (height: Int, width: Int) = options.run { outHeight to outWidth }
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        fun showNotification(
            isPlaying: Boolean,
            applicationContext: Context,
            packageManager: PackageManager,
            packageName: String,
            channelID: String,
            mMediaSessionCompat: MediaSessionCompat
        ): NotificationCompat.Builder {
            val playPauseIntent = if (isPlaying) Intent(
                applicationContext,
                MediaPlaybackService::class.java
            ).setAction("ACTION_PAUSE")
            else Intent(
                applicationContext,
                MediaPlaybackService::class.java
            ).setAction("ACTION_PLAY")
            val nextIntent =
                Intent(
                    applicationContext,
                    MediaPlaybackService::class.java
                ).setAction("ACTION_NEXT")
            val prevIntent = Intent(
                applicationContext,
                MediaPlaybackService::class.java
            ).setAction("ACTION_PREVIOUS")
            val intent = packageManager
                .getLaunchIntentForPackage(packageName)
                ?.setPackage(null)
                ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val activityIntent =
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            val builder = NotificationCompat.Builder(applicationContext, channelID).apply {
                val controller = mMediaSessionCompat.controller
                val mediaMetadata = controller.metadata
                addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_back,
                        applicationContext.getString(R.string.play_prev),
                        PendingIntent.getService(
                            applicationContext,
                            0,
                            prevIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                )
                val playOrPause = if (isPlaying) R.drawable.ic_pause
                else R.drawable.ic_play
                addAction(
                    NotificationCompat.Action(
                        playOrPause,
                        applicationContext.getString(R.string.play_pause),
                        PendingIntent.getService(
                            applicationContext, 0, playPauseIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                )
                addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_next,
                        applicationContext.getString(R.string.play_next),
                        PendingIntent.getService(
                            applicationContext,
                            0,
                            nextIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                )
                setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mMediaSessionCompat.sessionToken)
                )
                val smallIcon = if (isPlaying) R.drawable.play
                else R.drawable.pause

                setSmallIcon(smallIcon)
                setContentIntent(activityIntent)
                setContentTitle(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                setContentText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                setLargeIcon(mediaMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                priority = NotificationCompat.PRIORITY_DEFAULT
            }
            return builder
        }
    }
}