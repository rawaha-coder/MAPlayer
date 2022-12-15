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
import com.hybcode.maplayer.common.data.model.Song
import com.hybcode.maplayer.databinding.FragmentSongsBinding
import com.hybcode.maplayer.common.MusicViewModel

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
// TODO: Initialise the SongsAdapter class here
        musicViewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        musicViewModel.allSongs.observe(viewLifecycleOwner) { songs ->
            songs?.let {
                if (it.isNotEmpty() || completeLibrary.isNotEmpty()) processSongs(it)
            }
        }
// Shuffle the music library then play it
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
}