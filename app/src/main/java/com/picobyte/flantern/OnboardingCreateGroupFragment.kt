package com.picobyte.flantern

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.picobyte.flantern.databinding.FragmentOnboardingChatBinding
import com.picobyte.flantern.databinding.FragmentOnboardingCreateGroupBinding

class OnboardingCreateGroupFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentOnboardingCreateGroupBinding.inflate(inflater, container, false).root
    }
}