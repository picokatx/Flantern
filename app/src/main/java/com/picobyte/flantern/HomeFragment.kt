package com.picobyte.flantern

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.picobyte.flantern.adapters.HomeAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private val tabs = arrayOf("CHATS", "THREADS", "FEED")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.fragment_home, container, false)
        val viewPager = binding.findViewById<ViewPager2>(R.id.home_view_pager)
        val tabLayout = binding.findViewById<TabLayout>(R.id.home_tab_layout)
        val adapter = HomeAdapter(
            (binding.rootView.context as AppCompatActivity).supportFragmentManager,
            lifecycle
        )
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
            tab.view.width
        }.attach()
        return binding
    }

}