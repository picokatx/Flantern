package com.picobyte.flantern.wrappers

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.picobyte.flantern.adapters.ChatAdapter
import com.picobyte.flantern.types.MessageEdit
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PagedRecyclerWrapper<T>(
    val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
    val recycler: RecyclerView,
    val ref: DatabaseReference,
    val dataType: Class<T>,
    val repo: ArrayList<Pair<String, T>>,
    val pageLength: Int
) {
    lateinit var lastKey: String
    lateinit var firstKey: String
    //lateinit var liveKey: String
    var isLiveLoaded: Boolean = false

    fun initializePager() {
        ref.child("static").orderByKey().limitToLast(pageLength).get().addOnCompleteListener {
            if (it.result.children.toList().isNotEmpty()) {
                lastKey = it.result.children.first().key!!
                firstKey = it.result.children.last().key!!
                //liveKey = it.result.children.first().key!!
                it.result.children.forEach { msg ->
                    repo.add(Pair(msg.key!!, msg.getValue(dataType)!!))
                    adapter.notifyItemInserted(0)
                }
            }
        }
    }

    fun pageDown() {
        ref.child("static").orderByKey().startAfter(firstKey).limitToFirst(pageLength).get()
            .addOnCompleteListener {
                if (it.result.children.toList().isNotEmpty()) {
                    firstKey = it.result.children.last().key!!
                    it.result.children.forEach { msg ->
                        Log.e("Flantern", msg.key!!)
                        repo.add(Pair(msg.key!!, msg.getValue(dataType)!!))
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
                if (it.result.children.toList().isNotEmpty()) {
                    lastKey = it.result.children.first().key!!
                    it.result.children.reversed().forEach { msg ->
                        Log.e("Flantern", msg.key!!)
                        repo.add(0, Pair(msg.key!!, msg.getValue(dataType)!!))
                        adapter.notifyItemInserted(0)
                    }
                }
            }
        while (repo.size > pageLength * 3) {
            repo.removeAt(repo.size - 1)
            adapter.notifyItemRemoved(repo.size-1)
            firstKey = repo[0].first
        }
    }

    fun addItem(item: T) {
        val key = ref.child("static").push().key!!
        ref.child("static").child(key).setValue(item)
        ref.child("live").child(key).setValue(MessageEdit.ADD.ordinal)
    }

    fun removeItem(key: String) {
        ref.child("static").child(key).removeValue()
        ref.child("live").child(key).setValue(MessageEdit.DELETE.ordinal)
    }

    fun modifyItem(key: String, item: T) {
        ref.child("static").child(key).setValue(item)
        ref.child("live").child(key).setValue(MessageEdit.MODIFY.ordinal)
    }

    fun addItemListener() {
        ref.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (isLiveLoaded) {
                        when (snapshot.getValue(Int::class.java)) {
                            MessageEdit.ADD.ordinal -> {
                                ref.child("static/${snapshot.key}").get().addOnCompleteListener {
                                    repo.add(Pair(it.result.key!!, it.result.getValue(dataType)!!))

                                    adapter.notifyItemInserted(adapter.itemCount)
                                    recycler.scrollToPosition(adapter.itemCount - 1)
                                }
                            }
                            MessageEdit.DELETE.ordinal -> {
                                for (i in 0..repo.size) {
                                    if (repo[i].first == snapshot.key) {
                                        repo.removeAt(i)
                                        adapter.notifyItemRemoved(i)
                                        break
                                    }
                                }
                            }
                            MessageEdit.MODIFY.ordinal -> {
                                for (i in 0..repo.size) {
                                    if (repo[i].first == snapshot.key) {
                                        ref.child("static/${snapshot.key}").get()
                                            .addOnCompleteListener {
                                                repo[i] = Pair(
                                                    snapshot.key!!,
                                                    it.result.getValue(dataType)!!
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