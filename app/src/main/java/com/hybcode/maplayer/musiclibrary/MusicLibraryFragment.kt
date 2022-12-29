package com.hybcode.maplayer.musiclibrary

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
}