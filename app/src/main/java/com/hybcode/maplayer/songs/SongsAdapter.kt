package com.hybcode.maplayer.songs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.common.data.model.Song
import com.hybcode.maplayer.databinding.SongPreviewBinding
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class SongsAdapter(private val activity: MainActivity):
    RecyclerView.Adapter<SongsAdapter.SongsViewHolder>(), FastScrollRecyclerView.SectionedAdapter {
    var songs = mutableListOf<Song>()
    override fun getSectionName(position: Int): String {
        return songs[position].title[0].uppercaseChar().toString()
    }
    inner class SongsViewHolder(binding: SongPreviewBinding) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {
        internal var mArtwork = binding.artwork
        internal var mTitle = binding.title
        internal var mArtist = binding.artist
        internal var mMenu = binding.menu
        init {
            binding.root.isClickable = true
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener{
// TODO: Open options dialog
                return@setOnLongClickListener true
            }
        }
        override fun onClick(view: View) {
            activity.playNewSongs(songs, layoutPosition, false)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        val binding = SongPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongsViewHolder(binding)
    }
    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        val current = songs[position]
        activity.insertArtwork(current.albumID, holder.mArtwork)
        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
        holder.mMenu.setOnClickListener {
// TODO: Open options dialog
        }
    }
    override fun getItemCount() = songs.size}