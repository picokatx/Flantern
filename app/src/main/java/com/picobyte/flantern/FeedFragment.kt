package com.picobyte.flantern

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.picobyte.flantern.adapters.ChatsAdapter
import com.picobyte.flantern.adapters.FeedItem
import com.picobyte.flantern.adapters.FeedItemAdapter
import com.picobyte.flantern.databinding.FragmentFeedBinding
import com.picobyte.flantern.types.DatabaseOp
import com.picobyte.flantern.types.Group
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FeedFragment : Fragment() {
    val hardcodeCheck = HashMap<String, Boolean>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentFeedBinding = FragmentFeedBinding.inflate(inflater, container, false)
        val recyclerView = binding.review
        val layoutManager = LinearLayoutManager(requireActivity())
        val data: ArrayList<FeedItem> = ArrayList<FeedItem>()
        val adapter = FeedItemAdapter(data)
        val groupListeners = HashMap<String, ChildEventListener>()
        val userListeners = HashMap<String, ChildEventListener>()
        val groupMessageListeners = HashMap<String, ChildEventListener>()
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        val hardCodeUUID11 = UUID.randomUUID().toString()
        val hardCodeUUID12 = UUID.randomUUID().toString()
        val hardCodeUUID13 = UUID.randomUUID().toString()
        val hardCodeUUID14 = UUID.randomUUID().toString()
        val hardCodeUUID15 = UUID.randomUUID().toString()
        val hardCodeUUID16 = UUID.randomUUID().toString()
        val hardCodeUUID17 = UUID.randomUUID().toString()
        val hardCodeUUID18 = UUID.randomUUID().toString()
        val hardCodeUUID19 = UUID.randomUUID().toString()
        hardcodeCheck[hardCodeUUID11] = false
        hardcodeCheck[hardCodeUUID12] = false
        hardcodeCheck[hardCodeUUID13] = false
        hardcodeCheck[hardCodeUUID14] = false
        hardcodeCheck[hardCodeUUID15] = false
        hardcodeCheck[hardCodeUUID16] = false
        hardcodeCheck[hardCodeUUID17] = false
        hardcodeCheck[hardCodeUUID18] = false
        hardcodeCheck[hardCodeUUID19] = false

        val database = (context as MainActivity).rtDatabase
        val userUID = (context as MainActivity).authGoogle.getUID()
        val userContactsRef =
            (context as MainActivity).rtDatabase.getReference("user_contacts/$userUID/has")
        val userGroupsRef =
            (context as MainActivity).rtDatabase.getReference("user_groups/$userUID/has")

        userGroupsRef.child("static").get().addOnCompleteListener {
            it.result.children.forEach { child ->
                val groupUID = child.getValue(String::class.java)!!
                var groupName = "Group"
                database.getReference("groups/$groupUID/static/name").get()
                    .addOnCompleteListener { name ->
                        groupName = name.result.getValue(String::class.java)!!
                    }

            }
        }
        userGroupsRef.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    when (snapshot.child("op").getValue(Int::class.java)) {
                        DatabaseOp.ADD.ordinal -> {
                            userGroupsRef.child("static/${snapshot.key}").get()
                                .addOnCompleteListener {
                                    val groupUID = it.result.getValue(String::class.java)!!
                                    var groupName = "Group"
                                    Log.e("Flantern grouo", groupUID)
                                    database.getReference("groups/$groupUID/static/name").get()
                                        .addOnCompleteListener { name ->
                                            groupName = name.result.getValue(String::class.java)!!
                                            data.add(
                                                FeedItem(
                                                    "New Group",
                                                    "You joined group \"$groupName\".",
                                                    System.currentTimeMillis()
                                                )
                                            )
                                            adapter.notifyItemInserted(data.size-1)
                                            if (data.size > 48) {
                                                data.removeAt(0)
                                                adapter.notifyItemRemoved(0)
                                            }
                                            groupMessageListeners[groupUID] =
                                                database.getReference("group_messages/$groupUID/live")
                                                    .orderByKey().limitToLast(1)
                                                    .addChildEventListener(object : ChildEventListener {
                                                        override fun onChildAdded(
                                                            snapshot: DataSnapshot,
                                                            previousChildName: String?
                                                        ) {
                                                            if (!hardcodeCheck[hardCodeUUID19]!!) {
                                                                hardcodeCheck[hardCodeUUID19] = true
                                                            } else {
                                                                when (snapshot.child("op")
                                                                    .getValue(Int::class.java)) {
                                                                    DatabaseOp.ADD.ordinal -> {
                                                                        database.getReference("group_messages/$groupUID/static/${snapshot.key}/content")
                                                                            .get()
                                                                            .addOnCompleteListener { copium ->
                                                                                val description =
                                                                                    copium.result.getValue(String::class.java)!!
                                                                                data.add(FeedItem("New Message", description, System.currentTimeMillis()))
                                                                                adapter.notifyItemInserted(data.size-1)
                                                                                if (data.size > 48) {
                                                                                    data.removeAt(0)
                                                                                    adapter.notifyItemRemoved(0)
                                                                                }
                                                                            }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        override fun onChildChanged(
                                                            snapshot: DataSnapshot,
                                                            previousChildName: String?
                                                        ) {
                                                            return
                                                        }

                                                        override fun onChildRemoved(snapshot: DataSnapshot) {
                                                            return
                                                        }

                                                        override fun onChildMoved(
                                                            snapshot: DataSnapshot,
                                                            previousChildName: String?
                                                        ) {
                                                            return
                                                        }

                                                        override fun onCancelled(error: DatabaseError) {
                                                            return
                                                        }

                                                    })
                                        }
                                }
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    return
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    return
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    return
                }

                override fun onCancelled(error: DatabaseError) {
                    return
                }

            })

        return binding.root
    }
}