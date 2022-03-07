package com.picobyte.flantern.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.picobyte.flantern.ChatsFragment
import com.picobyte.flantern.FeedFragment
import com.picobyte.flantern.ThreadsFragment

private const val NUM_TABS = 3

class HomeAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return ChatsFragment()
            1 -> return ThreadsFragment()
            2 -> return FeedFragment()
        }
        return ChatsFragment()
    }

    public fun reload() {
        createFragment(0)
    }
}