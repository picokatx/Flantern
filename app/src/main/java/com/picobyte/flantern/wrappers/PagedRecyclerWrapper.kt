package com.picobyte.flantern.wrappers

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.picobyte.flantern.types.DatabaseOp
import kotlin.collections.ArrayList

class PagedRecyclerWrapper<T>(
    val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
    val recycler: RecyclerView,
    val ref: DatabaseReference,
    val type: Class<T>,
    val repo: ArrayList<Pair<String, T>>,
    val pageLength: Int
) {
    lateinit var lastKey: String
    lateinit var firstKey: String
    val live: ArrayList<Pair<String, T>> = ArrayList<Pair<String, T>>()
    lateinit var liveKey: String
    var isLiveLoaded: Boolean = false
    fun initializePager() {
        ref.child("static").orderByKey().limitToLast(pageLength).get().addOnCompleteListener {
            if (it.result.children.count() != 0) {
                lastKey = it.result.children.first().key!!
                firstKey = it.result.children.last().key!!
                liveKey = it.result.children.first().key!!
                it.result.children.forEach { msg ->
                    val msgKeyPair = Pair(msg.key!!, msg.getValue(type)!!)
                    repo.add(msgKeyPair)
                    adapter.notifyItemInserted(repo.size)
                }
                live.addAll(repo)
            }
        }
    }

    fun pageDown() {
        ref.child("static").orderByKey().startAfter(firstKey).limitToFirst(pageLength).get()
            .addOnCompleteListener {
                if (it.result.children.count() != 0) {
                    firstKey = it.result.children.last().key!!
                    it.result.children.forEach { msg ->
                        Log.e("Flantern", msg.key!!)
                        val msgKeyPair = Pair(msg.key!!, msg.getValue(type)!!)
                        repo.add(msgKeyPair)
                        adapter.notifyItemInserted(repo.size)
                    }
                }
            }
        while (repo.size > pageLength * 3) {
            repo.removeAt(0)
            adapter.notifyItemRemoved(0)
            lastKey = repo[0].first
        }
    }

    fun pageUp() {
        ref.child("static").orderByKey().endBefore(lastKey).limitToLast(pageLength).get()
            .addOnCompleteListener {
                if (it.result.children.count() != 0) {
                    lastKey = it.result.children.first().key!!
                    it.result.children.reversed().forEach { msg ->
                        val msgKeyPair = Pair(msg.key!!, msg.getValue(type)!!)
                        repo.add(0, msgKeyPair)
                        adapter.notifyItemInserted(0)
                    }
                }
            }
        while (repo.size > pageLength * 3) {
            repo.removeAt(repo.size - 1)
            adapter.notifyItemRemoved(repo.size - 1)
            firstKey = repo[repo.size - 1].first
        }
    }

    fun addItem(item: T) {
        val key = ref.child("static").push().key!!
        ref.child("static").child(key).setValue(item)
        ref.child("live/$key/op").setValue(DatabaseOp.ADD.ordinal)
    }

    fun removeItem(key: String) {
        ref.child("static/$key").get().addOnCompleteListener {
            if (it.result.exists()) {
                ref.child("static").child(key).removeValue()
                ref.child("live/$key/op").setValue(DatabaseOp.DELETE.ordinal)
                ref.child("live/$key/data").setValue(it.result.getValue(String::class.java))
            }
        }
    }

    fun modifyItem(key: String, item: T) {
        ref.child("static").child(key).setValue(item)
        ref.child("live/$key/op").setValue(DatabaseOp.MODIFY.ordinal)
    }

    fun addItemListener() {
        ref.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (isLiveLoaded) {
                        when (snapshot.child("op").getValue(Int::class.java)) {
                            DatabaseOp.ADD.ordinal -> {
                                ref.child("static/${snapshot.key}").get().addOnCompleteListener {
                                    val temp = Pair(it.result.key!!, it.result.getValue(type)!!)
                                    live.add(temp)
                                    live.removeAt(0)
                                    liveKey = live[0].first
                                    if (firstKey.compareTo(liveKey) >= 0) {
                                        Log.e("Flantern", "Program is moving you to the bottom")
                                        repo.add(temp)
                                        firstKey = temp.first
                                        adapter.notifyItemInserted(adapter.itemCount)
                                        recycler.scrollToPosition(adapter.itemCount - 1)
                                    } else {
                                        repo.clear()
                                        Log.e("Flantern", "Program is reloading this shit")
                                        repo.addAll(live)
                                        firstKey = repo[repo.size - 1].first
                                        lastKey = repo[0].first
                                        adapter.notifyDataSetChanged()
                                        recycler.scrollToPosition(adapter.itemCount - 1)
                                    }
                                }
                            }
                            DatabaseOp.DELETE.ordinal -> {
                                //todo: fix this in accordance to live/repo implementation
                                for (i in 0..repo.size) {
                                    if (repo[i].first == snapshot.key) {
                                        repo.removeAt(i)
                                        adapter.notifyItemRemoved(i)
                                        break
                                    }
                                }
                            }
                            DatabaseOp.MODIFY.ordinal -> {
                                //todo: fix this in accordance to live/repo implementation
                                for (i in 0..repo.size) {
                                    if (repo[i].first == snapshot.key) {
                                        ref.child("static/${snapshot.key}").get()
                                            .addOnCompleteListener {
                                                repo[i] = Pair(
                                                    snapshot.key!!,
                                                    it.result.getValue(type)!!
                                                )
                                                adapter.notifyItemChanged(i)
                                            }
                                        break
                                    }
                                }
                            }
                        }
                    } else {
                        isLiveLoaded = true
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
    }
}