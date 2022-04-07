package com.picobyte.flantern.wrappers

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.picobyte.flantern.types.DatabaseOp

class UserContactRecyclerWrapper<T>(
    val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
    val recycler: RecyclerView,
    val keyRef: DatabaseReference,
    val dataRef: DatabaseReference,
    val type: Class<T>,
    val repo: ArrayList<Pair<Pair<String, String>, T>>,
    val pageLength: Int
) {

    lateinit var lastKey: String
    lateinit var firstKey: String
    lateinit var liveKey: String
    val live: ArrayList<Pair<Pair<String, String>, T>> = ArrayList<Pair<Pair<String,String>, T>>()
    var isLiveLoaded: Boolean = false
    fun initializePager() {
        keyRef.child("static").orderByKey().limitToLast(pageLength).get().addOnCompleteListener {
            if (it.result.children.count() != 0) {
                lastKey = it.result.children.first().key!!
                firstKey = it.result.children.last().key!!
                liveKey = it.result.children.first().key!!
                it.result.children.forEach { entry ->
                    val key = entry.getValue(String::class.java)!!
                    Log.e("Flantern: key, ", key)
                    dataRef.child("${entry.key}/static").get().addOnCompleteListener { user ->
                        val userKeyPair = Pair(Pair(key, entry.key!!), user.result.getValue(type)!!)
                        repo.add(userKeyPair)
                        adapter.notifyItemInserted(repo.size)
                    }
                }
                live.addAll(repo)
            }
        }
    }

    fun pageDown() {
        keyRef.child("static").orderByKey().startAfter(firstKey).limitToFirst(pageLength).get()
            .addOnCompleteListener {
                if (it.result.children.count() != 0) {
                    firstKey = it.result.children.last().key!!
                    it.result.children.forEach { entry ->
                        val key = entry.getValue(String::class.java)!!
                        dataRef.child("${entry.key}/static").get().addOnCompleteListener { user ->
                            val userKeyPair = Pair(Pair(key, entry.key!!), user.result.getValue(type)!!)
                            repo.add(userKeyPair)
                            adapter.notifyItemInserted(repo.size)
                        }
                    }
                }
            }
        while (repo.size > pageLength * 3) {
            repo.removeAt(0)
            adapter.notifyItemRemoved(0)
            lastKey = repo[0].first.first
        }
    }

    fun pageUp() {
        keyRef.child("static").orderByKey().endBefore(lastKey).limitToLast(pageLength).get()
            .addOnCompleteListener {
                if (it.result.children.count() != 0) {
                    lastKey = it.result.children.first().key!!
                    it.result.children.reversed().forEach { entry ->
                        val key = entry.getValue(String::class.java)!!
                        dataRef.child("${entry.key}/static").get().addOnCompleteListener { user ->
                            val userKeyPair = Pair(Pair(key, entry.key!!), user.result.getValue(type)!!)
                            repo.add(0, userKeyPair)
                            adapter.notifyItemInserted(0)
                        }
                    }
                }
            }
        while (repo.size > pageLength * 3) {
            repo.removeAt(repo.size - 1)
            adapter.notifyItemRemoved(repo.size - 1)
            firstKey = repo[repo.size - 1].first.first
        }
    }

    fun addEntry(key: String) {
        val entryKey = keyRef.push().key!!
        keyRef.child("static/$entryKey").setValue(key)
        keyRef.child("live/$entryKey/op").setValue(DatabaseOp.ADD)
    }

    fun removeEntry(key: String) {
        keyRef.child("static/$key").get().addOnCompleteListener {
            if (it.result.exists()) {
                keyRef.child("static/$key").removeValue()
                keyRef.child("live/$key/op").setValue(DatabaseOp.DELETE)
                keyRef.child("live/$key/data").setValue(it.result.getValue(String::class.java))
            }
        }
    }

    fun addItemListener() {
        keyRef.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (isLiveLoaded) {
                        when (snapshot.child("op").getValue(Int::class.java)) {
                            DatabaseOp.ADD.ordinal -> {
                                keyRef.child("static/${snapshot.key}").get().addOnCompleteListener {
                                    val key = it.result.getValue(String::class.java)!!
                                    dataRef.child("${it.result.key}/static").get()
                                        .addOnCompleteListener { data ->
                                            val userKeyPair =
                                                Pair(Pair(key, snapshot.key!!), data.result.getValue(type)!!)
                                            live.add(userKeyPair)
                                            live.removeAt(0)
                                            liveKey = live[0].first.first
                                            if (firstKey > liveKey) {
                                                repo.add(userKeyPair)
                                                firstKey = userKeyPair.first.first
                                                adapter.notifyItemInserted(adapter.itemCount)
                                            }
                                        }
                                }
                            }
                            DatabaseOp.DELETE.ordinal -> {
                                //todo: fix this in accordance to live/repo implementation
                                for (i in 0..repo.size) {
                                    if (repo[i].first.first == snapshot.key) {
                                        repo.removeAt(i)
                                        adapter.notifyItemRemoved(i)
                                        break
                                    }
                                }
                            }
                            DatabaseOp.MODIFY.ordinal -> {
                                //todo: fix this in accordance to live/repo implementation
                                for (i in 0..repo.size) {
                                    if (repo[i].first.first == snapshot.key) {
                                        keyRef.child("static/${snapshot.key}").get()
                                            .addOnCompleteListener {
                                                val key = it.result.getValue(String::class.java)!!
                                                dataRef.child("${it.result.key}/static").get()
                                                    .addOnCompleteListener { item ->
                                                        repo[i] = Pair(
                                                            Pair(key, snapshot.key!!),
                                                            item.result.getValue(type)!!
                                                        )
                                                        adapter.notifyItemChanged(i)
                                                    }
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