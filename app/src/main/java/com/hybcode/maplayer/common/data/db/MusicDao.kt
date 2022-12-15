package com.hybcode.maplayer.common.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hybcode.maplayer.common.data.model.Song

@Dao
interface MusicDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: Song)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Query("SELECT * from music_table ORDER BY song_title ASC")
    fun getAllSongs(): LiveData<List<Song>>

    @Query("SELECT * from music_table WHERE song_album_id LIKE :albumID LIMIT 1")
    suspend fun getAlbum(albumID: String): List<Song>

    @Query("SELECT * FROM music_table WHERE song_title LIKE :search OR song_artist LIKE :search OR song_album LIKE :search")
    suspend fun searchSongs(search: String): List<Song>
}