package com.picobyte.flantern

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.picobyte.flantern.adapters.ChatsAdapter
import com.picobyte.flantern.databinding.FragmentChatsBinding
import com.picobyte.flantern.types.Group
import com.picobyte.flantern.types.Message
import com.picobyte.flantern.wrappers.FullLoadRecyclerWrapper
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper
import java.util.*
import kotlin.collections.ArrayList

class ChatsFragment : Fragment() {
    lateinit var pagedRecyclerWrapper: FullLoadRecyclerWrapper<Group>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentChatsBinding = FragmentChatsBinding.inflate(inflater, container, false)
        val recyclerView = binding.review
        val layoutManager = LinearLayoutManager(requireActivity())
        val groupsUID: ArrayList<Pair<Pair<String,String>, Group>> = ArrayList<Pair<Pair<String,String>, Group>>()
        val adapter = ChatsAdapter(groupsUID)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        val ref = (this.context as MainActivity).rtDatabase.getReference("/user_groups/${Firebase.auth.uid!!}/has")
        val dataRef = (this.context as MainActivity).rtDatabase.getReference("/groups")
        Log.e("Flantern", "hello chats am triggering")
        pagedRecyclerWrapper = FullLoadRecyclerWrapper<Group>(
            adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
            recyclerView,
            ref,
            dataRef,
            Group::class.java,
            groupsUID,
            16
        )
        pagedRecyclerWrapper.initializePager()
        pagedRecyclerWrapper.addItemListener()
        binding.review.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (layoutManager.findFirstCompletelyVisibleItemPosition() <= pagedRecyclerWrapper.pageLength - 1) {
                    pagedRecyclerWrapper.pageUp()
                } else if (layoutManager.findLastCompletelyVisibleItemPosition() >= pagedRecyclerWrapper.repo.size - pagedRecyclerWrapper.pageLength - 1) {
                    pagedRecyclerWrapper.pageDown()
                }
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        Log.e("Flantern", "listener is being removed")
        pagedRecyclerWrapper.removeItemListener()
    }
}