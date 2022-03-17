package com.picobyte.flantern

import android.R.attr
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
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
import android.graphics.Bitmap.CompressFormat

import android.R.attr.bitmap
import com.picobyte.flantern.types.Embed
import com.picobyte.flantern.types.EmbedType
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*


class ChatFragment : Fragment() {
    //lateinit var adapter: ChatAdapter
    //lateinit var lastKey: String
    //var isLiveLoaded: Boolean = false
    //var entryTime: Long = 0
    lateinit var pagedRecycler: PagedRecyclerWrapper<Message>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentChatBinding = FragmentChatBinding.inflate(inflater, container, false)
        val recyclerView = binding.recycler
        val layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.layoutManager = layoutManager
        val messagesUID: ArrayList<Pair<String, Message>> = ArrayList<Pair<String, Message>>()
        val adapter: ChatAdapter = ChatAdapter(messagesUID)
        val groupUID: String = arguments?.getString("group_uid")!!
        val ref =
            (requireActivity() as MainActivity).rtDatabase.getReference("/group_messages/$groupUID")
        pagedRecycler = PagedRecyclerWrapper<Message>(
            adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
            recyclerView,
            ref,
            Message::class.java,
            messagesUID,
            8
        )
        pagedRecycler.initializePager()
        pagedRecycler.addItemListener()
        binding.testSend.setOnClickListener {
            val timestamp = System.currentTimeMillis()
            val message = Message(
                Firebase.auth.uid!!,
                binding.editText.text.toString(),
                timestamp,
                null,
                false
            )
            pagedRecycler.addItem(message)
            binding.editText.setText("")
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                Log.e("Flantern", pagedRecycler.repo.size.toString())
                if (layoutManager.findFirstCompletelyVisibleItemPosition() <= pagedRecycler.pageLength - 1) {
                    pagedRecycler.pageUp()
                } else if (layoutManager.findLastCompletelyVisibleItemPosition() >= pagedRecycler.repo.size - pagedRecycler.pageLength - 1) {
                    Log.e("Flantern", "trying to page down")
                    pagedRecycler.pageDown()
                }
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
        recyclerView.adapter = adapter
        return binding.root

        /*val name = UUID.randomUUID()
        val drawable: BitmapDrawable = AppCompatResources.getDrawable(
            requireActivity(),
            R.mipmap.flantern_logo_foreground
        ) as BitmapDrawable
        val stream = ByteArrayOutputStream()
        drawable.bitmap.compress(CompressFormat.JPEG, 100, stream)
        (this.context as MainActivity).storage.getReference("$groupUID/$name.jpg").putStream(
            ByteArrayInputStream(stream.toByteArray())
        )*/
        /*val key = ref.child("static").push().key!!
        ref.child("static").child(key).setValue(message)
        ref.child("live").child(key).setValue(0)*/

        /*entryTime = System.currentTimeMillis()
        val binding: FragmentChatBinding = FragmentChatBinding.inflate(inflater, container, false)
        val recyclerView = binding.recycler
        val layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.layoutManager = layoutManager
        val messagesUID: ArrayList<Message> = ArrayList<Message>()
        val groupUID: String = arguments?.getString("group_uid")!!
        val ref =
            (requireActivity() as MainActivity).rtDatabase.getReference("/group_messages/$groupUID")
        adapter = ChatAdapter(messagesUID)

        binding.testSend.setOnClickListener {
            val timestamp = System.currentTimeMillis()

            val name = UUID.randomUUID()
            val drawable: BitmapDrawable = AppCompatResources.getDrawable(
                requireActivity(),
                R.mipmap.flantern_logo_foreground
            ) as BitmapDrawable
            val stream = ByteArrayOutputStream()
            drawable.bitmap.compress(CompressFormat.JPEG, 100, stream)
            (this.context as MainActivity).storage.getReference("$groupUID/$name.jpg").putStream(
                ByteArrayInputStream(stream.toByteArray())
            )

            val message = Message(
                Firebase.auth.uid!!,
                binding.editText.text.toString(),
                timestamp,
                null,
                false,
                Embed(EmbedType.IMAGE.ordinal, name.toString())
            )
            val key = ref.child("static").push().key!!
            ref.child("static").child(key).setValue(message)
            ref.child("live").child(key).setValue(0)
            binding.editText.setText("")
        }

        ref.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (isLiveLoaded) {
                        Log.e("Flantern", snapshot.key!!)
                        ref.child("static/${snapshot.key}").get().addOnCompleteListener {
                            messagesUID.add(it.result.getValue(Message::class.java)!!)
                            adapter.notifyItemInserted(adapter.itemCount)
                            recyclerView.scrollToPosition(adapter.itemCount - 1)
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

        ref.child("static").orderByKey().limitToLast(8).get().addOnCompleteListener {
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
                    ref.child("static").orderByKey().endBefore(lastKey).limitToLast(8).get()
                        .addOnCompleteListener {
                            Log.e("Flantner", lastKey)
                            if (it.result.children.toList().isNotEmpty()) {
                                lastKey = it.result.children.first().key!!
                                it.result.children.reversed().forEach { msg ->
                                    messagesUID.add(0, msg.getValue(Message::class.java)!!)
                                    adapter.notifyItemInserted(0)
                                }
                            }
                        }
                    Log.e("Flantern", "ello im at top")
                }
                super.onScrollStateChanged(recyclerView, newState)
            }
        })*/
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
    }

}