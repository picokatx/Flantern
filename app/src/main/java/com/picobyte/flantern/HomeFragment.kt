package com.picobyte.flantern

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.picobyte.flantern.adapters.HomeAdapter
import com.picobyte.flantern.databinding.DialogInviteCodeBinding
import com.picobyte.flantern.databinding.FragmentHomeBinding
import com.picobyte.flantern.types.DatabaseOp
import com.picobyte.flantern.types.RecyclableType
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateWithBundle
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
    ): View {
        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        val viewPager = binding.homeViewPager
        val tabLayout = binding.homeTabLayout
        //val authUID = (context as MainActivity).authGoogle.getUID()
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_new_contact -> {
                    navigateTo(binding.root, R.id.action_global_AddContactFragment)
                }
                R.id.action_new_group -> {
                    val bundle = Bundle()
                    bundle.putInt("type", RecyclableType.CONTACTS.ordinal)
                    bundle.putString(
                        "title_text",
                        (context as MainActivity).authGoogle.getDisplayName()
                    )
                    bundle.putString("subtitle_text", "Add Contact")
                    bundle.putString("user_uid", (context as MainActivity).authGoogle.getUID())
                    bundle.putInt("destination", R.id.action_global_EditGroupFragment)
                    navigateWithBundle(binding.root, R.id.action_global_ItemSelectFragment, bundle)
                }
                R.id.action_join_group -> {
                    val builder = AlertDialog.Builder(
                        ContextThemeWrapper(context, R.style.AlertDialogCustom)
                    )
                    val alertDialogView =
                        DialogInviteCodeBinding.inflate(inflater, container, false)
                    alertDialogView.join.setOnClickListener {
                        (context as MainActivity).requests.joinGroupWithCode(
                            alertDialogView.inviteCodeField.text.toString(),
                            {},
                            {
                                Toast.makeText(context, "This invite code is invalid!", Toast.LENGTH_LONG).show()
                            }
                        )
                        /*(requireActivity() as MainActivity).rtDatabase.getReference("/group_invites")
                            .child(
                                alertDialogView.inviteCodeField.text.toString()
                            ).get().addOnCompleteListener { code ->
                                if (code.result.exists()) {
                                    val groupUID = code.result.getValue(String::class.java)
                                    val groupUsersRef =
                                        (requireActivity() as MainActivity).rtDatabase.getReference("/group_users/$groupUID/has")
                                    val groupUsersKey = groupUsersRef.child("static").push().key
                                    groupUsersRef.child("static/$groupUsersKey").setValue(authUID)
                                    groupUsersRef.child("live/$groupUsersKey").setValue(DatabaseOp.ADD)
                                    val userRef =
                                        (requireActivity() as MainActivity).rtDatabase.getReference("/user_groups/$authUID/has")
                                    val userGroupsKey = groupUsersRef.child("static").push().key
                                    userRef.child("static/$userGroupsKey").setValue(groupUID)
                                    userRef.child("live/$userGroupsKey").setValue(DatabaseOp.ADD)

                                } else {
                                    Toast.makeText(context, "This invite code is invalid!", Toast.LENGTH_LONG).show()
                                }
                            }*/
                    }
                    builder.setMessage("Enter Invite Code")
                        .setView(alertDialogView.root)
                        .create()
                        .show()
                }
                R.id.action_starred_messages -> {

                }
                R.id.action_settings -> {
                    navigateTo(binding.root, R.id.action_global_SettingsFragment)
                }
            }
            true
        }
        val adapter = HomeAdapter(
            (context as AppCompatActivity).supportFragmentManager,
            lifecycle
        )
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
            tab.view.width
        }.attach()
        return binding.root
    }
}