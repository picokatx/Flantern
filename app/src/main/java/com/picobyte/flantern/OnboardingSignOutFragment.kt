package com.picobyte.flantern

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.picobyte.flantern.databinding.FragmentOnboardingSignOutBinding
import com.picobyte.flantern.utils.navigateTo

class OnboardingSignOutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentOnboardingSignOutBinding.inflate(inflater, container, false)
        binding.start.setOnClickListener {
            requireActivity().getSharedPreferences(
                "com.picobyte.flantern",
                AppCompatActivity.MODE_PRIVATE
            ).edit().putBoolean("onboard", false).apply()
            navigateTo(binding.root, R.id.action_global_SignInFragment)
        }
        return binding.root
    }
}