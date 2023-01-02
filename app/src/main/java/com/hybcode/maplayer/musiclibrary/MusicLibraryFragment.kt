package com.hybcode.maplayer.musiclibrary


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.hybcode.maplayer.NavGraphDirections
import com.hybcode.maplayer.databinding.FragmentMusicLibraryBinding



class MusicLibraryFragment : Fragment() {

    private var _binding : FragmentMusicLibraryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadSongsProgressBar.isVisible = false

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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}