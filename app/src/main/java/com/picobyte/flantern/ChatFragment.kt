package com.picobyte.flantern

import android.Manifest
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
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.opengl.Visibility
import android.provider.MediaStore
import android.view.ViewAnimationUtils
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.picobyte.flantern.types.Embed
import com.picobyte.flantern.types.EmbedType
import com.picobyte.flantern.types.Group
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper
import java.util.*
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt
import android.graphics.BitmapFactory

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import com.github.tcking.giraffecompressor.GiraffeCompressor
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateWithBundle
import java.io.*
import java.net.URI


class ChatFragment : Fragment() {
    lateinit var pagedRecycler: PagedRecyclerWrapper<Message>
    var imageURI: Uri = Uri.EMPTY
    var audioURI: Uri = Uri.EMPTY
    val galleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                imageURI = result.data!!.data!!
            }
        }
    }
    val audioLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                audioURI = result.data!!.data!!
            }
        }
    }

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
        val groupUID: String = arguments?.getString("group_uid")!!
        val adapter: ChatAdapter = ChatAdapter(groupUID, messagesUID)
        binding.attachContainer.visibility = View.INVISIBLE
        binding.attachBtn.setOnClickListener {
            val cx = (binding.attachBtn.x + binding.attachBtn.width / 2).roundToInt()
            val cy = (binding.attachContainer.y + binding.attachBtn.height).roundToInt()
            val finalRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
            if (binding.attachContainer.visibility == View.INVISIBLE) {
                binding.attachContainer.visibility = View.VISIBLE
                ViewAnimationUtils.createCircularReveal(
                    binding.attachContainer,
                    cx,
                    cy,
                    0f,
                    finalRadius
                ).start()
            } else {
                val anim = ViewAnimationUtils.createCircularReveal(
                    binding.attachContainer,
                    cx,
                    cy,
                    finalRadius,
                    0f
                )
                anim.doOnEnd {
                    binding.attachContainer.visibility = View.INVISIBLE
                }
                anim.start()
            }
        }
        binding.topBarBack.setOnClickListener {
            navigateTo(binding.root, R.id.action_global_HomeFragment)
        }
        binding.topbar.setOnClickListener {

        }
        binding.attachImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                )
                galleryLauncher.launch(intent)
            }
        }
        binding.attachAudio.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Audio.Media.INTERNAL_CONTENT_URI
                )
                galleryLauncher.launch(intent)
            }

        }
        val groupRef =
            (requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID/static")
        Log.e("Flantern", groupRef.toString())
        groupRef.get().addOnCompleteListener {
            val group = it.result.getValue(Group::class.java)!!
            binding.topBarTitle.text = group.name
            binding.topBarSubtitle.text = group.description
            if (group.profile != null) {
                (context as MainActivity).storage.getReference("$groupUID/${group.profile}.jpg")
                    .getBytes(1000 * 1000).addOnCompleteListener { image ->
                        binding.topBarIcon.setImageBitmap(
                            BitmapFactory.decodeByteArray(
                                image.result,
                                0,
                                image.result.size
                            )
                        )
                    }
            } else {
                binding.topBarIcon.setImageResource(R.mipmap.flantern_logo_foreground)
            }
        }
        binding.topBarTitle.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("group_uid", groupUID)
            navigateWithBundle(binding.root, R.id.action_global_ChatDetailsFragment, bundle)
        }
        val ref =
            (requireActivity() as MainActivity).rtDatabase.getReference("/group_messages/$groupUID")
        pagedRecycler = PagedRecyclerWrapper<Message>(
            adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
            recyclerView,
            ref,
            Message::class.java,
            messagesUID,
            16
        )
        pagedRecycler.initializePager()
        pagedRecycler.addItemListener()
        binding.testSend.setOnClickListener {
            val timestamp = System.currentTimeMillis()
            var embed: Embed? = null
            if (imageURI != Uri.EMPTY) {
                val embedUUID = UUID.randomUUID().toString()
                if (context!!.contentResolver.getType(imageURI) == "video/mp4") {
                    GiraffeCompressor.create().input(File(imageURI.path!!))
                    embed = Embed(EmbedType.VIDEO.ordinal, embedUUID)
                } else if (context!!.contentResolver.getType(imageURI) == "image/jpeg") {
                    val outputStream = ByteArrayOutputStream()
                    BitmapFactory.decodeStream(
                        this.context!!.contentResolver.openInputStream(
                            imageURI
                        )!!
                    )
                        .compress(CompressFormat.JPEG, 10, outputStream)
                    (this.context as MainActivity).storage.getReference("$groupUID/$embedUUID.jpg")
                        .putBytes(
                            outputStream.toByteArray()
                        )
                }
                embed = Embed(EmbedType.IMAGE.ordinal, embedUUID)
                imageURI = Uri.EMPTY
            }
            val message = Message(
                Firebase.auth.uid!!,
                binding.editText.text.toString(),
                timestamp,
                null,
                false,
                embed
            )
            pagedRecycler.addItem(message)
            binding.editText.setText("")
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                Log.e("Flantern page size", pagedRecycler.repo.size.toString())
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