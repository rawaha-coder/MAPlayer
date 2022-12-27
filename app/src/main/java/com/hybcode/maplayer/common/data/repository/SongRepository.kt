package com.hybcode.maplayer.common.data.repository

import androidx.lifecycle.LiveData
import com.hybcode.maplayer.common.data.contentresolver.ContentResolverHelper
import com.hybcode.maplayer.common.data.contentresolver.MusicLibrary
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.common.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongRepository(private val contentResolverHelper: ContentResolverHelper,
                     private val musicLibrary: MusicLibrary
){
    suspend fun getSongData(): List<Song> = withContext(Dispatchers.IO){
        contentResolverHelper.getSongData()
    }

    suspend fun getMusicLibrary(): List<Song> = withContext(Dispatchers.IO){
        musicLibrary.getSongData()
    }
}