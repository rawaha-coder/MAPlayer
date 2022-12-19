package com.hybcode.maplayer.search

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.data.MusicDatabase
import com.hybcode.maplayer.databinding.FragmentSearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private var musicDatabase: MusicDatabase? = null
    private var searchView: SearchView? = null
    private lateinit var callingActivity: MainActivity
    private lateinit var searchAdapter: SearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        callingActivity = activity as MainActivity
        musicDatabase = MusicDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchAdapter = SearchAdapter(callingActivity)
        binding.recyclerView.adapter = searchAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val searchItem = menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView
        val onQueryListener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                search("%$newText%")
                return true
            }
            override fun onQueryTextSubmit(query: String): Boolean = true
        }

        searchView?.apply {
                    isIconifiedByDefault = false
                    queryHint = getString(R.string.search_hint)
                    setOnQueryTextListener(onQueryListener)
                }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun search(query: String) = lifecycleScope.launch(Dispatchers.IO) {
        val songs = musicDatabase!!.musicDao().searchSongs(query).take(10)
        lifecycleScope.launch(Dispatchers.Main) {
            val adapterSongs = searchAdapter.songs
            when {
                songs.isEmpty() -> {
                    val previousSongCount = searchAdapter.songs.size
                    searchAdapter.songs = mutableListOf()
                    searchAdapter.notifyItemRangeRemoved(0, previousSongCount)
                    binding.noResults.visibility = View.VISIBLE
                }
                adapterSongs.isEmpty() -> {
                binding.noResults.visibility = View.GONE
                searchAdapter.songs = songs.toMutableList()
                searchAdapter.notifyItemRangeInserted(0, songs.size)
                }
                else -> {
                binding.noResults.visibility = View.GONE
                val removeItems = adapterSongs - songs.toSet()
                val addItems = songs - adapterSongs.toSet()
                    for (s in removeItems) {
                    val index = searchAdapter.songs.indexOfFirst {
                        it.songID == s.songID
                        }
                    searchAdapter.songs.removeAt(index)
                    searchAdapter.notifyItemRemoved(index)
                    searchAdapter.notifyItemChanged(index)
                }
                    for (s in addItems) {
                            val index = songs.indexOfFirst {
                                it.songID == s.songID
                            }
                            searchAdapter.songs.add(index, s)
                            searchAdapter.notifyItemInserted(index)
                            searchAdapter.notifyItemChanged(index)
                        }
            }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        callingActivity.hideKeyboard(callingActivity)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}