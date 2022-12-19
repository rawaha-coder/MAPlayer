package com.hybcode.maplayer.playback

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.domain.model.Song
import com.hybcode.maplayer.databinding.FragmentCurrentlyPlayingBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.fragment.findNavController
import android.widget.PopupMenu
import androidx.navigation.findNavController

class CurrentlyPlayingFragment : Fragment() {
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private var currentlyPlayingSong: Song? = null
    private var _binding: FragmentCurrentlyPlayingBinding? = null
    private val binding get() = _binding!!
    private var fastForwarding = false
    private var fastRewinding = false
    private lateinit var callingActivity: MainActivity
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        sharedElementReturnTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrentlyPlayingBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
// The currently playing fragment will overlay the active fragment from the
// mobile_navigation navigation graph. We need to intercept touch events
// that would otherwise reach the underlying fragment
        binding.root.setOnTouchListener { _, _ ->
            return@setOnTouchListener true
        }
        // get information about the currently playing song
        playbackViewModel.currentlyPlayingSong.observe(viewLifecycleOwner) { song ->
            song?.let {
                currentlyPlayingSong = it
                binding.title.text = it.title
                binding.album.text = it.album
                binding.artist.text = it.artist
                callingActivity.insertArtwork(it.albumID, binding.artwork)
            }
        }
// check whether a song is currently playing
        playbackViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            playing?.let {
                if (it) binding.btnPlay.setImageResource(R.drawable.ic_pause)
                else binding.btnPlay.setImageResource(R.drawable.ic_play)
            }
        }
// keep track of currently playing song duration
        playbackViewModel.currentPlaybackDuration.observe(viewLifecycleOwner) { duration ->
            duration?.let {
                binding.currentSeekBar.max = it
                binding.currentMax.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
            }
        }
        // keep track of currently playing song position
        playbackViewModel.currentPlaybackPosition.observe(viewLifecycleOwner) { position ->
            position?.let {
                binding.currentSeekBar.progress = position
                binding.currentPosition.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
            }
        }
// toggle play/pause
        binding.btnPlay.setOnClickListener { callingActivity.playPauseControl() }
// restart or play previous song when backward button is clicked
        binding.btnBackward.setOnClickListener {
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
        // skip to next song when forward button is pressed
        binding.btnForward.setOnClickListener {
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

        val shuffleSetting = sharedPreferences.getBoolean("shuffle", false)
        if (shuffleSetting)
            binding.currentButtonShuffle.setColorFilter(
            ContextCompat.getColor(callingActivity,
            R.color.teal_700))
        binding.currentButtonShuffle.setOnClickListener{
            if (callingActivity.shuffleCurrentPlayQueue())
                binding.currentButtonShuffle.setColorFilter(ContextCompat.getColor(callingActivity,
                    R.color.teal_700))
            else binding.currentButtonShuffle.setColorFilter(ContextCompat.getColor(requireActivity(),
                R.color.purple_200))
        }
        var repeatSetting = sharedPreferences.getInt("repeat", REPEAT_MODE_NONE)

        when (repeatSetting) {
            REPEAT_MODE_ALL -> binding.currentButtonRepeat.setColorFilter(ContextCompat.getColor(callingActivity,
                R.color.teal_700))
            REPEAT_MODE_ONE -> {
                binding.currentButtonRepeat.setColorFilter(ContextCompat.getColor(callingActivity,
                    R.color.teal_700))
                binding.currentButtonRepeat.setImageDrawable(ContextCompat.getDrawable(requireActivity(),
                    R.drawable.ic_repeat_one))
            }
        }
        binding.currentButtonRepeat.setOnClickListener{
                    when (repeatSetting) {
                        REPEAT_MODE_NONE -> {
                            repeatSetting = REPEAT_MODE_ALL
                            binding.currentButtonRepeat.setColorFilter(ContextCompat.getColor(callingActivity,
                                R.color.teal_700))
                            callingActivity.setRepeatMode(repeatSetting)
                            Toast.makeText(requireActivity(), "Repeat play queue", Toast.LENGTH_SHORT).show()
                        }
                        REPEAT_MODE_ALL -> {
                        repeatSetting = REPEAT_MODE_ONE
                        binding.currentButtonRepeat.setImageDrawable(ContextCompat.getDrawable(requireActivity(),
                            R.drawable.ic_repeat_one))
                        callingActivity.setRepeatMode(repeatSetting)
                        Toast.makeText(requireActivity(), "Repeat current song", Toast.LENGTH_SHORT).show()
                    }
                        REPEAT_MODE_ONE -> {
                        repeatSetting = REPEAT_MODE_NONE
                        binding.currentButtonRepeat.setImageDrawable(ContextCompat.getDrawable(requireActivity(),
                            R.drawable.ic_repeat))
                        binding.currentButtonRepeat.setColorFilter(ContextCompat.getColor(requireActivity(),
                            R.color.purple_200))
                        callingActivity.setRepeatMode(repeatSetting)
                        Toast.makeText(requireActivity(), "Repeat mode off", Toast.LENGTH_SHORT).show()
                    }
                    }
                }
        binding.currentClose.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.artwork.setOnClickListener {
                    showPopup(binding.currentClose)
                }
        binding.currentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) callingActivity.seekTo(progress)
                    }
                })
    }

    override fun onResume() {
        super.onResume()
        callingActivity.hideSystemBars(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        callingActivity.hideSystemBars(false)
    }

    private fun showPopup(view: View) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.currently_playing_menu)
            setForceShowIcon(true)
            setOnDismissListener {
                callingActivity.hideSystemBars(true)
            }
            setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.search -> {
                                findNavController().popBackStack()
                                callingActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_search)
                            }R
                            .id.queue -> {
                            findNavController().popBackStack()
                            callingActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_queue)
                        }}
                true
                    }
            show()
        }
    }
}