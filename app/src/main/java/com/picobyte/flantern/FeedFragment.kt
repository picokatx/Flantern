package com.picobyte.flantern

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.picobyte.flantern.adapters.ChatsAdapter
import com.picobyte.flantern.adapters.FeedItem
import com.picobyte.flantern.adapters.FeedItemAdapter
import com.picobyte.flantern.api.FlanternRequests
import com.picobyte.flantern.databinding.FragmentFeedBinding
import com.picobyte.flantern.types.DatabaseOp
import com.picobyte.flantern.types.Group
import com.picobyte.flantern.types.GroupEdit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FeedFragment : Fragment() {
    val hardcodeCheck = HashMap<String, Boolean>()
    val data: ArrayList<FeedItem> = ArrayList<FeedItem>()
    val groupListeners = HashMap<String, ChildEventListener>()
    val userListeners = HashMap<String, ChildEventListener>()
    val groupMessageListeners = HashMap<String, ChildEventListener>()
    lateinit var database: FirebaseDatabase
    lateinit var req: FlanternRequests
    lateinit var adapter: FeedItemAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentFeedBinding = FragmentFeedBinding.inflate(inflater, container, false)
        val recyclerView = binding.review
        val layoutManager = LinearLayoutManager(requireActivity())
        adapter = FeedItemAdapter(data)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        req = (context as MainActivity).requests
        database = (context as MainActivity).rtDatabase
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
        userGroupsRef.child("static").get().addOnCompleteListener {
            for (i in it.result.children) {
                val groupUID = i.getValue(String::class.java)!!
                var groupName = "Group"
                Log.e("Flantern grouo", groupUID)
                database.getReference("groups/$groupUID/static/name").get()
                    .addOnCompleteListener { name ->
                        groupName =
                            name.result.getValue(String::class.java)!!
                        groupListeners[groupUID] =
                            database.getReference("groups/$groupUID/live")
                                .orderByKey()
                                .limitToLast(1)
                                .addChildEventListener(object :
                                    ChildEventListener {
                                    var temp = false
                                    override fun onChildAdded(
                                        snapshot: DataSnapshot,
                                        previousChildName: String?
                                    ) {
                                        if (!temp) {
                                            temp = true
                                        } else {
                                            when (snapshot.child("op")
                                                .getValue(Int::class.java)) {
                                                GroupEdit.NAME.ordinal -> {
                                                    database.getReference("user/$groupUID/static/name")
                                                        .get()
                                                        .addOnCompleteListener { data ->
                                                            val name =
                                                                data.result.getValue(
                                                                    String::class.java
                                                                )!!
                                                            updateFeed(
                                                                FeedItem(
                                                                    "Group Name Changed",
                                                                    "$groupName changed their name to $name",
                                                                    System.currentTimeMillis()
                                                                )
                                                            )
                                                            groupName = name
                                                        }
                                                }
                                                GroupEdit.DESCRIPTION.ordinal -> {
                                                    database.getReference("user/$groupUID/static/description")
                                                        .get()
                                                        .addOnCompleteListener { data ->
                                                            val description =
                                                                data.result.getValue(
                                                                    String::class.java
                                                                )
                                                            updateFeed(
                                                                FeedItem(
                                                                    "Group Name Changed",
                                                                    "$groupName changed their description to $description",
                                                                    System.currentTimeMillis()
                                                                )
                                                            )
                                                        }
                                                }
                                                GroupEdit.PROFILE.ordinal -> {
                                                    updateFeed(
                                                        FeedItem(
                                                            "Group Profile Changed",
                                                            "$groupName updated their group profile",
                                                            System.currentTimeMillis()
                                                        )
                                                    )
                                                }
                                                GroupEdit.DELETED.ordinal -> {
                                                    updateFeed(
                                                        FeedItem(
                                                            "Group Deleted",
                                                            "Group \"$groupName\" was deleted",
                                                            System.currentTimeMillis()
                                                        )
                                                    )
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
                        groupMessageListeners[groupUID] =
                            database.getReference("group_messages/$groupUID/live")
                                .orderByKey().limitToLast(1)
                                .addChildEventListener(object : ChildEventListener {
                                    var temp = false
                                    override fun onChildAdded(
                                        snapshot: DataSnapshot,
                                        previousChildName: String?
                                    ) {
                                        if (!temp) {
                                            temp = true
                                        } else {
                                            when (snapshot.child("op")
                                                .getValue(Int::class.java)) {
                                                DatabaseOp.ADD.ordinal -> {
                                                    database.getReference("group_messages/$groupUID/static/${snapshot.key}")
                                                        .get()
                                                        .addOnCompleteListener { copium ->
                                                            val userUID =
                                                                copium.result.child("user")
                                                                    .getValue(String::class.java)!!
                                                            val description =
                                                                copium.result.child("content")
                                                                    .getValue(String::class.java)!!
                                                            req.getUser(userUID, { user ->
                                                                updateFeed(
                                                                    FeedItem(
                                                                        "New Message in $groupName",
                                                                        "${user.name}: $description",
                                                                        System.currentTimeMillis()
                                                                    )
                                                                )
                                                            }, {
                                                                updateFeed(
                                                                    FeedItem(
                                                                        "New Message in $groupName",
                                                                        "User: $description",
                                                                        System.currentTimeMillis()
                                                                    )
                                                                )

                                                            })
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
        userGroupsRef.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                var temp = false
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (!temp) {
                        temp = true
                    } else {
                        when (snapshot.child("op").getValue(Int::class.java)) {
                            DatabaseOp.ADD.ordinal -> {
                                userGroupsRef.child("static/${snapshot.key}").get()
                                    .addOnCompleteListener {
                                        val groupUID = it.result.getValue(String::class.java)!!
                                        var groupName = "Group"
                                        Log.e("Flantern grouo", groupUID)
                                        database.getReference("groups/$groupUID/static/name").get()
                                            .addOnCompleteListener { name ->
                                                groupName =
                                                    name.result.getValue(String::class.java)!!
                                                updateFeed(
                                                    FeedItem(
                                                        "New Group",
                                                        "You joined group \"$groupName\".",
                                                        System.currentTimeMillis()
                                                    )
                                                )
                                                groupListeners[groupUID] =
                                                    database.getReference("groups/$groupUID/live")
                                                        .orderByKey()
                                                        .limitToLast(1)
                                                        .addChildEventListener(object :
                                                            ChildEventListener {
                                                            var temp = false
                                                            override fun onChildAdded(
                                                                snapshot: DataSnapshot,
                                                                previousChildName: String?
                                                            ) {
                                                                if (!temp) {
                                                                    temp = true
                                                                } else {
                                                                    when (snapshot.child("op")
                                                                        .getValue(Int::class.java)) {
                                                                        GroupEdit.NAME.ordinal -> {
                                                                            database.getReference("user/$groupUID/static/name")
                                                                                .get()
                                                                                .addOnCompleteListener { data ->
                                                                                    val name =
                                                                                        data.result.getValue(
                                                                                            String::class.java
                                                                                        )!!
                                                                                    updateFeed(
                                                                                        FeedItem(
                                                                                            "Group Name Changed",
                                                                                            "$groupName changed their name to $name",
                                                                                            System.currentTimeMillis()
                                                                                        )
                                                                                    )
                                                                                    groupName = name
                                                                                }
                                                                        }
                                                                        GroupEdit.DESCRIPTION.ordinal -> {
                                                                            database.getReference("user/$groupUID/static/description")
                                                                                .get()
                                                                                .addOnCompleteListener { data ->
                                                                                    val description =
                                                                                        data.result.getValue(
                                                                                            String::class.java
                                                                                        )
                                                                                    updateFeed(
                                                                                        FeedItem(
                                                                                            "Group Name Changed",
                                                                                            "$groupName changed their description to $description",
                                                                                            System.currentTimeMillis()
                                                                                        )
                                                                                    )
                                                                                }
                                                                        }
                                                                        GroupEdit.PROFILE.ordinal -> {
                                                                            updateFeed(
                                                                                FeedItem(
                                                                                    "Group Profile Changed",
                                                                                    "$groupName updated their group profile",
                                                                                    System.currentTimeMillis()
                                                                                )
                                                                            )
                                                                        }
                                                                        GroupEdit.DELETED.ordinal -> {
                                                                            updateFeed(
                                                                                FeedItem(
                                                                                    "Group Deleted",
                                                                                    "Group \"$groupName\" was deleted",
                                                                                    System.currentTimeMillis()
                                                                                )
                                                                            )
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

                                                groupMessageListeners[groupUID] =
                                                    database.getReference("group_messages/$groupUID/live")
                                                        .orderByKey().limitToLast(1)
                                                        .addChildEventListener(object :
                                                            ChildEventListener {
                                                            var temp = false
                                                            override fun onChildAdded(
                                                                snapshot: DataSnapshot,
                                                                previousChildName: String?
                                                            ) {
                                                                if (!temp) {
                                                                    temp = true
                                                                } else {
                                                                    when (snapshot.child("op")
                                                                        .getValue(Int::class.java)) {
                                                                        DatabaseOp.ADD.ordinal -> {
                                                                            database.getReference("group_messages/$groupUID/static/${snapshot.key}")
                                                                                .get()
                                                                                .addOnCompleteListener { copium ->
                                                                                    val userUID =
                                                                                        copium.result.child("user")
                                                                                            .getValue(String::class.java)!!
                                                                                    val description =
                                                                                        copium.result.child("content")
                                                                                            .getValue(String::class.java)!!
                                                                                    req.getUser(userUID, { user ->
                                                                                        updateFeed(
                                                                                            FeedItem(
                                                                                                "New Message in $groupName",
                                                                                                "${user.name}: $description",
                                                                                                System.currentTimeMillis()
                                                                                            )
                                                                                        )
                                                                                    }, {
                                                                                        updateFeed(
                                                                                            FeedItem(
                                                                                                "New Message in $groupName",
                                                                                                "User: $description",
                                                                                                System.currentTimeMillis()
                                                                                            )
                                                                                        )
                                                                                    })
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

    override fun onDestroy() {
        for (i in userListeners) {
            database.getReference("user/${i.key}/live").removeEventListener(i.value)
        }
        for (i in groupListeners) {
            database.getReference("groups/${i.key}/live").removeEventListener(i.value)
        }
        for (i in groupMessageListeners) {
            database.getReference("group_messages/${i.key}/live").removeEventListener(i.value)
        }
        super.onDestroy()
    }

    fun updateFeed(item: FeedItem) {
        data.add(0, item)
        adapter.notifyItemInserted(0)
        if (data.size > 48) {
            data.removeAt(data.size-1)
            adapter.notifyItemRemoved(data.size)
        }
    }
}