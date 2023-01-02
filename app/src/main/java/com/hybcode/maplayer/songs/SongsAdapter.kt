package com.hybcode.maplayer.songs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.databinding.SongPreviewBinding

class SongsAdapter(): RecyclerView.Adapter<SongsAdapter.SongsViewHolder>() {

    var songs = mutableListOf<Song>()

    inner class SongsViewHolder(private val binding: SongPreviewBinding):
        RecyclerView.ViewHolder(binding.root){
        fun bind(song: Song) {
            binding.title.text = song.title
            binding.artist.text = song.artist
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        val binding =SongPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        val song: Song = songs[position]
        holder.bind(song)
        holder.itemView.setOnClickListener {
            //fragment.playNewSongs(song)
        }
    }

    override fun getItemCount() = songs.size
}