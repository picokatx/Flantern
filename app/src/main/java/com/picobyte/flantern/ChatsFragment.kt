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
        recyclerView.layoutManager = layoutManager
        val groupsUID: ArrayList<Pair<Pair<String,String>, Group>> = ArrayList<Pair<Pair<String,String>, Group>>()
        val adapter = ChatsAdapter(groupsUID)
        recyclerView.adapter = adapter
        val ref = (this.context as MainActivity).rtDatabase.getReference("/user_groups/${Firebase.auth.uid!!}/has")
        val dataRef = (this.context as MainActivity).rtDatabase.getReference("/groups")

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

        /*(this.context as MainActivity).rtDatabase.getReference("/user_groups/${Firebase.auth.uid!!}/has/static")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    groupsUID.add(0, snapshot.key!!)
                    adapter.notifyItemInserted(0)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    adapter.notifyItemChanged(groupsUID.indexOf(snapshot.key))
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val pos: Int = groupsUID.indexOf(snapshot.key!!)
                    groupsUID.remove(snapshot.key!!)
                    adapter.notifyItemRemoved(pos)
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    return
                }

                override fun onCancelled(error: DatabaseError) {
                    return
                }

            })*/
        /*(requireActivity() as MainActivity).rtDatabase.getReference("/user_groups/${Firebase.auth.uid!!}/has")
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
            })*/
        return binding.root
    }
}