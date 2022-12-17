package com.hybcode.maplayer.library

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hybcode.maplayer.playqueue.PlayQueueFragment
import com.hybcode.maplayer.songs.SongsFragment

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) PlayQueueFragment()
        else SongsFragment()
    }
}