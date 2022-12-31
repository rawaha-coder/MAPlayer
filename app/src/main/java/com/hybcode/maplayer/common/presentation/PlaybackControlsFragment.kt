package com.hybcode.maplayer.common.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.databinding.FragmentPlaybackControlsBinding

class PlaybackControlsFragment : Fragment() {

    private var _binding: FragmentPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    private val songViewModel: SongViewModel by viewModels()
    private var currentlyPlayingSong: Song? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaybackControlsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//// get information about the currently playing song
//        songViewModel.currentlyPlayingSong.observe(viewLifecycleOwner) { song ->
//            song?.let {
//                currentlyPlayingSong = it
//                binding.title.text = it.title
//                binding.album.text = it.album
//                binding.artist.text = it.artist
//            }
//        }
//// check whether a song is currently playing
//        songViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
//            playing?.let {
//                if (it) binding.btnPlay.setImageResource(R.drawable.ic_pause)
//                else binding.btnPlay.setImageResource(R.drawable.ic_play)
//            }
//        }
//// keep track of currently playing song duration
//        songViewModel.currentPlaybackDuration.observe(viewLifecycleOwner) { duration ->
//            duration?.let {
//                binding.songProgressBar.max = it
//                binding.currentMax.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
//            }
//        }
//// keep track of currently playing song position
//        songViewModel.currentPlaybackPosition.observe(viewLifecycleOwner) { position ->
//            position?.let {
//                binding.songProgressBar.progress = position
//                binding.currentPosition.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
//            }
//        }
    }

}