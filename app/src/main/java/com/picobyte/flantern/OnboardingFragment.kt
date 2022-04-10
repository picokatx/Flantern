package com.picobyte.flantern

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.picobyte.flantern.adapters.OnboardingViewPagerAdapter
import com.picobyte.flantern.databinding.FragmentOnboardingBinding
import com.picobyte.flantern.utils.navigateTo

class OnboardingFragment: Fragment() {
    private val fragmentList = ArrayList<Fragment>()
    private lateinit var prefs: SharedPreferences
    private lateinit var binding: FragmentOnboardingBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        prefs = context?.getSharedPreferences("com.picobyte.flantern",
            AppCompatActivity.MODE_PRIVATE
        )!!
        var test = arguments?.getString("bypass")
        if (prefs.contains("onboard") && test==null) {
            navigateTo(binding.root, R.id.action_global_SignInFragment)
        } else {
            fragmentList.add(OnboardingWelcomeFragment())
            fragmentList.add(OnboardingAddContactFragment())
            fragmentList.add(OnboardingCreateGroupFragment())
            fragmentList.add(OnboardingChatFragment())
            fragmentList.add(OnboardingSignOutFragment())
            binding.mainViewPager.adapter = OnboardingViewPagerAdapter(requireActivity(), fragmentList)
            binding.mainViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            binding.mainIndicator.setViewPager(binding.mainViewPager)
        }
        return binding.root
    }

}