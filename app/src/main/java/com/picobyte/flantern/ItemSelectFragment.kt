package com.picobyte.flantern

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.picobyte.flantern.adapters.ChatsAdapter
import com.picobyte.flantern.adapters.UserAdapter
import com.picobyte.flantern.databinding.FragmentItemSelectBinding
import com.picobyte.flantern.types.Group
import com.picobyte.flantern.types.RecyclableType
import com.picobyte.flantern.types.SelectableType
import com.picobyte.flantern.types.User
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateUp
import com.picobyte.flantern.utils.navigateWithBundle
import com.picobyte.flantern.wrappers.FullLoadRecyclerWrapper
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper
import com.picobyte.flantern.wrappers.UserContactRecyclerWrapper

class ItemSelectFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentItemSelectBinding.inflate(inflater, container, false)
        binding.topBarTitle.text = arguments?.getString("title_text")!!
        binding.topBarSubtitle.text = arguments?.getString("subtitle_text")!!
        binding.itemRecycler.layoutManager = LinearLayoutManager(context)
        binding.topBarBack.setOnClickListener {
            navigateUp(binding.root)
        }
        val userUID = arguments?.getString("user_uid")!!
        val destination = arguments?.getInt("destination")!! // R.id.global_fragment_to_fragment
        val destBundle = arguments?.getBundle("destination_bundle")
        when (arguments?.getInt("type")!!) {
            RecyclableType.CONTACTS.ordinal -> {
                val usersUID: ArrayList<Pair<Pair<String, String>, User>> = ArrayList<Pair<Pair<String, String>, User>>()
                val adapter: UserAdapter = UserAdapter(usersUID)
                adapter.selectable = SelectableType.MULTI
                binding.itemRecycler.adapter = adapter
                val ref =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/user_contacts/$userUID/has")
                val userRef =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/user")
                val pagedRecycler = FullLoadRecyclerWrapper<User>(
                    adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
                    binding.itemRecycler,
                    ref,
                    userRef,
                    User::class.java,
                    usersUID,
                    16
                )
                pagedRecycler.initializePager()
                pagedRecycler.addItemListener()
                binding.proceed.setOnClickListener {
                    if (destBundle!=null) {
                        destBundle.putStringArrayList("user_contacts", adapter.selected)
                        navigateWithBundle(binding.root, destination, destBundle)
                    } else {
                        val bundle = Bundle()
                        bundle.putStringArrayList("user_contacts", adapter.selected)
                        navigateWithBundle(binding.root, destination, bundle)
                    }
                }
            }
            RecyclableType.GROUPS.ordinal -> {
                val usersUID: ArrayList<Pair<Pair<String, String>, Group>> = ArrayList<Pair<Pair<String, String>, Group>>()
                val adapter: ChatsAdapter = ChatsAdapter(usersUID)
                adapter.selectable = SelectableType.MULTI
                binding.itemRecycler.adapter = adapter
                val ref =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/user_groups/${userUID}/has")
                val groupRef =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/groups")
                val groupPagedRecyclerWrapper = FullLoadRecyclerWrapper<Group>(
                    adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
                    binding.itemRecycler,
                    ref,
                    groupRef,
                    Group::class.java,
                    usersUID,
                    16
                )
                groupPagedRecyclerWrapper.initializePager()
                groupPagedRecyclerWrapper.addItemListener()
                binding.proceed.setOnClickListener {
                    if (destBundle!=null) {
                        destBundle.putStringArrayList("user_groups", adapter.selected)
                        navigateWithBundle(binding.root, destination, destBundle)
                    } else {
                        val bundle = Bundle()
                        bundle.putStringArrayList("user_groups", adapter.selected)
                        navigateWithBundle(binding.root, destination, bundle)
                    }
                }

            }
            RecyclableType.THREADS.ordinal -> {
                //todo: add threads implementation (adapter)
                val threadRef =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/user_threads/${userUID}/has")
            }
        }
        return binding.root
    }
}