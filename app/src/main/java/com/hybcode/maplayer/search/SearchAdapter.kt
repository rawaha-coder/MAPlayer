package com.hybcode.maplayer.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song

class SearchAdapter(private val activity: MainActivity):
    RecyclerView.Adapter<SearchAdapter.SongsViewHolder>() {
    var songs = mutableListOf<Song>()
    inner class SongsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
        internal var mMenu = itemView.findViewById<ImageButton>(R.id.menu)
        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener {
                activity.showSongPopup(it, songs[layoutPosition])
                return@setOnLongClickListener true
            }
        }
        override fun onClick(view: View) {
            activity.playNewSongs(listOf(songs[layoutPosition]), 0, false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        return SongsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.song_preview, parent, false))
    }

    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        val current = songs[position]
        activity.insertArtwork(current.albumID, holder.mArtwork)
        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
        holder.mMenu.setOnClickListener {
            activity.showSongPopup(it, current)
        }
    }

    override fun getItemCount() = songs.size
}