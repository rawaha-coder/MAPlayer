package com.hybcode.maplayer.controls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.R
import com.hybcode.maplayer.databinding.FragmentControlsBinding
import com.hybcode.maplayer.playback.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ControlsFragment : Fragment() {

    private var _binding: FragmentControlsBinding? = null
    private val binding get() = _binding!!
    private var fastForwarding = false
    private var fastRewinding = false
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlsBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playbackViewModel.currentlyPlayingSong.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.title.text = it.title
                binding.artist.text = it.artist
                binding.album.text = it.album
                callingActivity.insertArtwork(it.albumID, binding.artwork)
                binding.root.setOnClickListener {
                    val extras = FragmentNavigatorExtras(
                        binding.artwork to binding.artwork.transitionName,
                        binding.title to binding.title.transitionName,
                        binding.album to binding.album.transitionName,
                        binding.artist to binding.artist.transitionName,
                        binding.btnPlay to binding.btnPlay.transitionName,
                        binding.btnBackward to binding.btnBackward.transitionName,
                        binding.btnForward to binding.btnForward.transitionName
                    )
                    findNavController().navigate(R.id.nav_currently_playing, null, null, extras)
                }
            } else {
                binding.title.text = null
                binding.artist.text = null
                binding.album.text = null
                Glide.with(callingActivity)
                    .clear(binding.artwork)
                binding.root.setOnClickListener(null)
            }
        }
        playbackViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            isPlaying?.let {
                if (it) binding.btnPlay.setImageResource(R.drawable.ic_pause)
                else binding.btnPlay.setImageResource(R.drawable.ic_play)
            }
        }
        playbackViewModel.currentPlaybackPosition.observe(viewLifecycleOwner) { position ->
            position?.let {
                binding.songProgressBar.progress = position
            }
        }
// keep track of currently playing song duration
        playbackViewModel.currentPlaybackDuration.observe(viewLifecycleOwner) { duration ->
            duration?.let {
                binding.songProgressBar.max = it
            }
        }

        // play/pause btn actions
        binding.btnPlay.setOnClickListener {
            callingActivity.playPauseControl()
        }
        binding.btnBackward.setOnClickListener{
                    if (fastRewinding) fastRewinding = false
                    else callingActivity.skipBack()
                }
        binding.btnBackward.setOnLongClickListener {
                    fastRewinding = true
                    lifecycleScope.launch {
                        do {
                            callingActivity.fastRewind()
                            delay(500)
                        } while (fastRewinding)
                    }
                    return@setOnLongClickListener false
                }
        binding.btnForward.setOnClickListener{
                    if (fastForwarding) fastForwarding = false
                    else callingActivity.skipForward()
                }
        binding.btnForward.setOnLongClickListener {
                    fastForwarding = true
                    lifecycleScope.launch {
                        do {
                            callingActivity.fastForward()
                            delay(500)
                        } while (fastForwarding)
                    }
            return@setOnLongClickListener false
                }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.songProgressBar.max = playbackViewModel.currentPlaybackDuration.value ?: 0
        binding.songProgressBar.progress = playbackViewModel.currentPlaybackPosition.value ?: 0
    }
}