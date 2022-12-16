package com.hybcode.maplayer.playqueue

import android.annotation.SuppressLint
import android.app.ProgressDialog.show
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.R
import com.hybcode.maplayer.databinding.FragmentPlayQueueBinding
import com.hybcode.maplayer.playback.PlaybackViewModel

class PlayQueueFragment : Fragment() {

    private var _binding: FragmentPlayQueueBinding? = null
    private val binding get() = _binding!!
    private lateinit var callingActivity: MainActivity
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private lateinit var playQueueAdapter: PlayQueueAdapter

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ACTION_STATE_DRAG) viewHolder?.itemView?.alpha = 0.5f
                }
                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    viewHolder.itemView.alpha = 1.0f
                    playbackViewModel.currentPlayQueue.value = playQueueAdapter.playQueue
                }
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target:
                RecyclerView.ViewHolder): Boolean {
                    val from = viewHolder.layoutPosition
                    val to = target.layoutPosition
                    if (from != to) {
                        val song = playQueueAdapter.playQueue[from]
                        playQueueAdapter.playQueue.removeAt(from)
                        playQueueAdapter.playQueue.add(to, song)
                        playQueueAdapter.notifyItemMoved(from, to)
                    }
                    return true
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayQueueBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.layoutManager = LinearLayoutManager(activity)
        binding.root.itemAnimator = DefaultItemAnimator()
        playQueueAdapter = PlayQueueAdapter(this, callingActivity)
        binding.root.adapter = playQueueAdapter

        playbackViewModel.currentPlayQueue.observe(viewLifecycleOwner) { queue ->
            queue?.let {
                if (playQueueAdapter.playQueue.size > it.size) {
// Song(s) removed from the play queue
                    val difference = playQueueAdapter.playQueue - it.toSet()
                    for (item in difference) {
                        val index = playQueueAdapter.playQueue.indexOfFirst { queueItem ->
                            queueItem.first == item.first
                        }
                        if (index != -1) {
                            playQueueAdapter.playQueue.removeAt(index)
                            playQueueAdapter.notifyItemRemoved(index)
                        }
                    }
                } else {
// Adapter loaded from scratch or play queue shuffled
                    playQueueAdapter.playQueue = it.toMutableList()
                    playQueueAdapter.notifyDataSetChanged()
                }
            }
        }

        playbackViewModel.currentlyPlayingQueueID.observe(viewLifecycleOwner) { position ->
            position?.let { playQueueAdapter.currentlyPlayingSongChanged(it) }
        }

        itemTouchHelper.attachToRecyclerView(binding.root)
    }

    override fun onResume() {
        super.onResume()
// This code finds the position in the recycler view list of the currently playing song, and scrolls to it
        val currentlyPlayingQueueIndex = playQueueAdapter.playQueue.indexOfFirst {queueItem ->
            queueItem.first == playQueueAdapter.currentlyPlayingQueueID
        }
        if (currentlyPlayingQueueIndex != -1) (binding.root.layoutManager as
                LinearLayoutManager).scrollToPositionWithOffset(currentlyPlayingQueueIndex, 0)
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)

    fun showPopup(view: View, index: Int) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.queue_item_menu)
            setOnMenuItemClickListener {
                if (it.itemId == R.id.remove_item) callingActivity.removeQueueItem(index)
                true
            }
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}