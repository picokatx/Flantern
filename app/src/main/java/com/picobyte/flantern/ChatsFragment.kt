package com.picobyte.flantern

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.picobyte.flantern.adapters.ChatsAdapter
import com.picobyte.flantern.databinding.FragmentChatsBinding
import com.picobyte.flantern.types.Group
import com.picobyte.flantern.types.Message
import java.util.*
import kotlin.collections.ArrayList

class ChatsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentChatsBinding = FragmentChatsBinding.inflate(inflater, container, false)
        val recyclerView = binding.review
        val layoutManager = GridLayoutManager(requireActivity(), 1)
        recyclerView.layoutManager = layoutManager
        val groupsUID: ArrayList<String> = ArrayList<String>()
        val adapter = ChatsAdapter(groupsUID)

        (requireActivity() as MainActivity).rtDatabase.getReference("/user_groups/${Firebase.auth.uid!!}/has")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        if (!groupsUID.contains(it.key!!)) {
                            groupsUID.add(0, it.key!!)
                            adapter.notifyItemInserted(0)
                        }
                    }
                    var i = 0
                    while (i < groupsUID.size) {
                        if (!snapshot.hasChild("/${groupsUID[i]}")) {
                            groupsUID.remove(groupsUID[i])
                            adapter.notifyItemRemoved(i)
                        } else {
                            i++
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    return
                }
            })
        recyclerView.adapter = adapter
        return binding.root
    }
}