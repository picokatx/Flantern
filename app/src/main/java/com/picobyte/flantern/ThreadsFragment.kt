package com.picobyte.flantern

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.picobyte.flantern.databinding.FragmentThreadsBinding

class ThreadsFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentThreadsBinding = FragmentThreadsBinding.inflate(inflater,container,false)
        binding.endServiceBtn.setOnClickListener {
            val service = Intent(context, FeedService::class.java)
            context!!.stopService(service)
        }
        return binding.root
    }
}