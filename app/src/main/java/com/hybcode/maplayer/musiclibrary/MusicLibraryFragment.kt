package com.hybcode.maplayer.musiclibrary

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.hybcode.maplayer.NavGraphDirections
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.databinding.FragmentMusicLibraryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MusicLibraryFragment : Fragment() {

    private var _binding : FragmentMusicLibraryBinding? = null
    private val binding get() = _binding!!
    private val musicLibraryViewModel: MusicLibraryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadSongsProgressBar.isVisible = true

        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner) { songs ->
            songs?.let {
                Toast.makeText(context, it.size.toString(), Toast.LENGTH_SHORT).show()
                if (it.isNotEmpty()) {
                    processSongs(it)
                binding.loadSongsProgressBar.isVisible = false

                }
            }
        }

        binding.allSongsButton.setOnClickListener {
            val action = NavGraphDirections.actionNavMusicLibraryToNavAllSongs()
            view.findNavController().navigate(action)
        }

        binding.playListButton.setOnClickListener {
            val action = NavGraphDirections.actionNavMusicLibraryToNavPlayListsLibrary()
            view.findNavController().navigate(action)
        }

        binding.albumLibraryButton.setOnClickListener {
            val action = NavGraphDirections.actionNavMusicLibraryToNavAlbumsLibrary()
            view.findNavController().navigate(action)
        }

        binding.artistLibraryButton.setOnClickListener {
            val action = NavGraphDirections.actionNavMusicLibraryToNavArtistsLibrary()
            view.findNavController().navigate(action)
        }
    }

    private fun processSongs(songList: List<Song>){
        getSongListSize(songList)
        getArtistSize(songList)
        getAlbumSize(songList)
        getPlayListSize()
    }

    private fun getAlbumSize(songList: List<Song>) {

    }

    private fun getArtistSize(songList: List<Song>) {

    }

    @SuppressLint("SetTextI18n")
    private fun getSongListSize(songList: List<Song>) {
        binding.allSongsButton.text = "${getString(R.string.music_library)} (${songList.size.toString()})"
    }

    private fun getPlayListSize() {
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}