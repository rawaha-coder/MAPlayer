package com.hybcode.maplayer.songs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.common.presentation.SongViewModel
import com.hybcode.maplayer.databinding.FragmentSongsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!

    private val songViewModel: SongViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return inflater.inflate(R.layout.fragment_songs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateScreenState()
    }

    private fun updateScreenState() {
        binding.loadSongsProgressBar.isVisible = true
    }

    fun playNewSongs(song: Song) = songViewModel.playMedia(song)

}