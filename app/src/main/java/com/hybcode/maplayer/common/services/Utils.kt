package com.hybcode.maplayer.common.services

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import androidx.core.app.NotificationCompat
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

fun getMediaMetadata(song: Song?, application : Application, applicationContext: Context): MediaMetadataCompat =
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

