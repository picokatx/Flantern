package com.picobyte.flantern

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.picobyte.flantern.databinding.FragmentContactBinding
import com.picobyte.flantern.utils.navigateUp

class ContactFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentContactBinding.inflate(inflater, container, false)
        binding.topBarBack.setOnClickListener {
            navigateUp(binding.root)
        }
        return binding.root
    }
}