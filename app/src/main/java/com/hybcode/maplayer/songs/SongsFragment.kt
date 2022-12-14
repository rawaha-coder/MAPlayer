package com.hybcode.maplayer.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.databinding.FragmentSongsBinding
import com.hybcode.maplayer.common.presentation.MusicViewModel
import java.util.*

class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private var completeLibrary = mutableListOf<Song>()
    private var isProcessing = false
    private lateinit var songsAdapter: SongsAdapter
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        songsAdapter = SongsAdapter(callingActivity)
        binding.recyclerView.adapter = songsAdapter
        songsAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        musicViewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        musicViewModel.allSongs.observe(viewLifecycleOwner) { songs ->
            songs?.let {
                if (it.isNotEmpty() || completeLibrary.isNotEmpty()) processSongs(it)
            }
        }

        binding.fab.setOnClickListener {
            callingActivity.playNewSongs(completeLibrary, 0, true)
        }
        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (dy > 0 && binding.fab.visibility == View.VISIBLE) binding.fab.hide()
                        else if (dy < 0 && binding.fab.visibility != View.VISIBLE) binding.fab.show()
                    }
                })
    }

    private fun processSongs(songList: List<Song>) {
        if (!isProcessing) {
            isProcessing = true
            completeLibrary = songList.sortedBy { song ->
                song.title.uppercase(Locale.ROOT)
            }.toMutableList()
            val songs = songsAdapter.songs
            songsAdapter.songs = completeLibrary
            when {
                songs.isEmpty() -> songsAdapter.notifyItemRangeInserted(0, completeLibrary.size)
                completeLibrary.size > songs.size -> {
                    val difference = completeLibrary - songs.toSet()
                    for (s in difference) {
                        val index = completeLibrary.indexOfFirst {
                            it.songID == s.songID
                        }
                        if (index != -1) songsAdapter.notifyItemInserted(index)
                    }
                }
                completeLibrary.size < songs.size -> {
                val difference = songs - completeLibrary.toSet()
                for (s in difference) {
                    val index = songs.indexOfFirst {
                        it.songID == s.songID
                    }
                    if (index != -1) songsAdapter.notifyItemRemoved(index)
                }
            }
            }
            isProcessing = false
        }
    }
}