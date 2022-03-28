package com.picobyte.flantern

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.picobyte.flantern.databinding.FragmentItemSelectBinding
import com.picobyte.flantern.types.RecyclableType

class ItemSelectFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentItemSelectBinding.inflate(inflater, container, false)
        binding.topBarTitle.text = arguments?.getString("title_text")!!
        binding.topBarSubtitle.text = arguments?.getString("subtitle_text")!!
        val userUID = arguments?.getInt("user_uid")!!
        when (arguments?.getInt("type")!!) {
            RecyclableType.CONTACTS.ordinal -> {
                
            }
            RecyclableType.GROUPS.ordinal -> {

            }
            RecyclableType.THREADS.ordinal -> {

            }
        }
        return binding.root
    }
}