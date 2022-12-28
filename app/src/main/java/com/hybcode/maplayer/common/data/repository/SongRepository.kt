package com.hybcode.maplayer.common.data.repository

import com.hybcode.maplayer.common.data.contentresolver.ContentResolverHelper
import com.hybcode.maplayer.common.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SongRepository @Inject constructor(private val contentResolverHelper: ContentResolverHelper
){
    suspend fun getSongData(): List<Song> = withContext(Dispatchers.IO){
        contentResolverHelper.getSongData()
    }

}