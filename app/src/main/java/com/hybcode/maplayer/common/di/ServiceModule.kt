package com.hybcode.maplayer.common.di

import android.content.Context
import android.media.AudioAttributes


object ServiceModule {

    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()


}