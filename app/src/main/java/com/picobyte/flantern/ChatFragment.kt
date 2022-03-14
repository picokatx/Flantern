package com.picobyte.flantern

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.picobyte.flantern.adapters.ChatAdapter
import com.picobyte.flantern.types.Message
import com.picobyte.flantern.databinding.FragmentChatBinding
import kotlin.collections.ArrayList
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.paging.DatabasePagingOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError


class ChatFragment : Fragment() {
    lateinit var adapter: ChatAdapter
    lateinit var lastKey: String
    var entryTime: Long = 0
    var msgCount: Int = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        entryTime = System.currentTimeMillis()
        val binding: FragmentChatBinding = FragmentChatBinding.inflate(inflater, container, false)
        val recyclerView = binding.recycler
        val layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.layoutManager = layoutManager
        val messagesUID: ArrayList<Message> = ArrayList<Message>()
        val groupUID: String = arguments?.getString("group_uid")!!
        //layoutManager.reverseLayout = true
        val ref =
            (requireActivity() as MainActivity).rtDatabase.getReference("/group_messages/$groupUID")
        adapter = ChatAdapter(messagesUID)
        binding.testSend.setOnClickListener {
            val timestamp = System.currentTimeMillis()
            val message = Message(
                Firebase.auth.uid!!,
                binding.editText.text.toString(),
                timestamp,
                null,
                false
            )
            ref.child(ref.push().key!!).setValue(message)
            (requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID/recent")
                .setValue(message)
            binding.editText.setText("")
            recyclerView.scrollToPosition(adapter.itemCount - 1);
        }
        ref.orderByKey().limitToLast(8).get().addOnCompleteListener {
            lastKey = it.result.children.first().key!!
            it.result.children.reversed().forEach { msg ->
                messagesUID.add(0, msg.getValue(Message::class.java)!!)
                adapter.notifyItemInserted(0)
            }
        }

        //When at top of recycler, get paged data once from db, display in recyclerview
        //When data changed, push to live reference, listener for live applies changes
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                Log.e("Flantern", layoutManager.findFirstCompletelyVisibleItemPosition().toString())
                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    ref.orderByKey().endBefore(lastKey).limitToLast(8).get().addOnCompleteListener {
                        Log.e("Flantner", lastKey)
                        lastKey = it.result.children.first().key!!
                        it.result.children.reversed().forEach { msg ->
                            messagesUID.add(0, msg.getValue(Message::class.java)!!)
                            adapter.notifyItemInserted(0)
                        }
                    }
                    Log.e("Flantern", "ello im at top")
                }
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
        /*val options: FirebaseRecyclerOptions<Message> = FirebaseRecyclerOptions.Builder<Message>()
            .setQuery(ref, Message::class.java)
            .build()
        val pagingOptions: DatabasePagingOptions<Message> = DatabasePagingOptions.Builder<Message>()
            .setLifecycleOwner(requireActivity())
            .setQuery(ref, PagingConfig(4), Message::class.java)
            .build()
        adapter = ChatAdapter(pagingOptions)
*/
        /*ref.limitToLast(1).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.e("Flantern: dbupdate", snapshot.key!!)
                if (msgCount <= 4) {
                    msgCount++
                } else {

                    adapter.notifyItemInserted(adapter.itemCount)
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
        })*/

        /*ref.orderByKey().limitToLast(10).get().addOnCompleteListener { page ->
            //Loads initial page
            page.result.children.forEach { first ->
                Log.e("Flantern", first.key!!)
                messagesUID.add(0, first.getValue(Message::class.java)!!)
                adapter.notifyItemInserted(0)
            }
            //Loads other pages
            binding.recycler.addOnScrollListener(object :
                OnScrolledToTop(layoutManager, page.result.children.first().key!!) {
                override fun onLoadMore(current_page: Int) {
                    Log.e("Flantern", "onLoadMore")
                    ref.orderByKey().endBefore(current_page.toString()).limitToLast(1).get()
                        .addOnCompleteListener { page ->
                            if (page.result.exists()) {
                                page.result.children.forEach { msg ->
                                    messagesUID.add(0, msg.getValue(Message::class.java)!!)
                                    adapter.notifyItemInserted(0)
                                }
                                this.prevEntryNamespace = page.result.children.first().key!!
                            }
                        }
                }
            })
        }*/

        /*if (layoutManager.findFirstVisibleItemPosition() == 0) {
            ref.orderByKey().endBefore(it.result.key).limitToLast(1).get().addOnCompleteListener {

            }
        }
        ref.orderByKey().limitToLast(1).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                messagesUID.add(snapshot.key!!)
                adapter.notifyItemInserted(messagesUID.size - 1)

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                adapter.notifyItemChanged(messagesUID.indexOf(snapshot.key))
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
        })*/
        recyclerView.adapter = adapter
        return binding.root
    }
}