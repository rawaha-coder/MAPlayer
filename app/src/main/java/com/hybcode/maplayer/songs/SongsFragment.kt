package com.hybcode.maplayer.songs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet.GONE
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.common.presentation.SongViewModel
import com.hybcode.maplayer.databinding.FragmentSongsBinding
import com.hybcode.maplayer.musiclibrary.MusicLibraryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private lateinit var songsAdapter: SongsAdapter
    private val musicLibraryViewModel: MusicLibraryViewModel by viewModels()
    private var completeLibrary = mutableListOf<Song>()
    private var isProcessing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loadSongsProgressBar.isVisible = true
        binding.songsRecyclerView.layoutManager = LinearLayoutManager(activity)
        binding.songsRecyclerView.itemAnimator = DefaultItemAnimator()
        songsAdapter = SongsAdapter(this)
        binding.songsRecyclerView.adapter = songsAdapter
        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner) { songs ->
                songs?.let {
                    if (it.isNotEmpty()){
                        processSongs(it)
                        binding.loadSongsProgressBar.visibility = View.GONE
                    }
                }
            }
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

    fun playNewSongs(song: Song){
        musicLibraryViewModel.playMedia(song)
    }

}