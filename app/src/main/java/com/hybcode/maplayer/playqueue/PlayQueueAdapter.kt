package com.hybcode.maplayer.playqueue

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song

class PlayQueueAdapter(private val fragment: PlayQueueFragment, private val activity: MainActivity):
    RecyclerView.Adapter<PlayQueueAdapter.PlayQueueViewHolder>() {
    var currentlyPlayingQueueID = -1
    var playQueue = mutableListOf<Pair<Int, Song>>()
    inner class PlayQueueViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
        internal var mHandle = itemView.findViewById<ImageView>(R.id.handle)
        internal var mMenu = itemView.findViewById<ImageButton>(R.id.menu)
        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
        }
        override fun onClick(view: View) {
            activity.skipToQueueItem(layoutPosition)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayQueueViewHolder {return PlayQueueViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.queue_item, parent, false))
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlayQueueViewHolder, position: Int) {
        val currentSong = playQueue[position].second
        holder.mTitle.text = currentSong.title
        holder.mArtist.text = currentSong.artist
        val emphasisColour = if (playQueue[position].first == currentlyPlayingQueueID) {
            ContextCompat.getColor(activity, R.color.purple_500)
        } else ContextCompat.getColor(activity, R.color.teal_700)
        holder.mTitle.setTextColor(emphasisColour)
        holder.mArtist.setTextColor(emphasisColour)
        holder.mHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
            return@setOnTouchListener true
        }
        holder.mMenu.setOnClickListener {
            fragment.showPopup(it, position)
                }
    }
    override fun getItemCount() = playQueue.size

    fun currentlyPlayingSongChanged(newQueueID: Int) {
        val oldCurrentlyPlayingIndex = playQueue.indexOfFirst {
            it.first == currentlyPlayingQueueID
        }
        currentlyPlayingQueueID = newQueueID
        if (oldCurrentlyPlayingIndex != -1) notifyItemChanged(oldCurrentlyPlayingIndex)
        val newCurrentlyPlayingIndex = playQueue.indexOfFirst {
            it.first == currentlyPlayingQueueID
        }
        notifyItemChanged(newCurrentlyPlayingIndex)
    }
}