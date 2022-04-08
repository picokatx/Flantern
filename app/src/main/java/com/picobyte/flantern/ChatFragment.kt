package com.picobyte.flantern

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.picobyte.flantern.adapters.ChatAdapter
import com.picobyte.flantern.types.Message
import com.picobyte.flantern.databinding.FragmentChatBinding
import kotlin.collections.ArrayList

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.picobyte.flantern.types.Embed
import com.picobyte.flantern.types.EmbedType
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper
import java.util.*
import kotlin.math.hypot
import kotlin.math.roundToInt

import android.net.Uri
import com.github.tcking.giraffecompressor.GiraffeCompressor
import com.picobyte.flantern.utils.getMimeType
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateWithBundle
import rx.android.schedulers.AndroidSchedulers
import java.io.*
import android.view.*
import android.view.Menu
import android.widget.Toast
import androidx.core.net.toUri


class ChatFragment : Fragment() {
    lateinit var pagedRecycler: PagedRecyclerWrapper<Message>
    var documentURI: Uri = Uri.EMPTY
    var documentType: EmbedType? = null
    private val galleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                documentURI = result.data!!.data!!
                documentType = EmbedType.IMAGE
            }
        }
    }
    private val audioLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                documentURI = result.data!!.data!!
                documentType = EmbedType.AUDIO
            }
        }
    }
    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                documentURI = result.data!!.data!!
                documentType = EmbedType.IMAGE
            }
        }
    }

    val documentLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                documentURI = result.data!!.data!!
                documentType = EmbedType.DOCUMENT
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
        val adapter: ChatAdapter = ChatAdapter(groupUID, messagesUID, container!!)
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
                intent.type = "image/*";
                galleryLauncher.launch(intent)
            }
        }
        binding.attachVideo.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                )
                intent.type = "video/*";
                galleryLauncher.launch(intent)
            }
        }
        binding.attachProfile.setOnClickListener {

        }
        binding.attachCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE
                )
                cameraLauncher.launch(intent)
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
                audioLauncher.launch(intent)
            }
        }
        binding.attachDocument.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(
                    Intent.ACTION_GET_CONTENT,
                )
                documentLauncher.launch(intent)
            }
        }
        (context as MainActivity).requests.getGroup(groupUID, {
            binding.topBarTitle.text = it.name
            binding.topBarSubtitle.text = it.description
            if (it.profile != null) {
                (context as MainActivity).requests.getGroupMediaBitmap(
                    it.profile,
                    groupUID,
                    { bitmap ->
                        binding.topBarIcon.setImageBitmap(bitmap)
                    })
            } else {
                binding.topBarIcon.setImageResource(R.mipmap.flantern_logo_foreground)
            }
        }, {
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_LONG
            ).show()
        })
        val groupRef =
            (requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID/static")
        /*groupRef.get().addOnCompleteListener {
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
        }*/
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
            val extension = getMimeType(requireContext(), documentURI)
            if (documentURI != Uri.EMPTY) {
                val embedUUID = UUID.randomUUID().toString()
                when (documentType) {
                    EmbedType.IMAGE -> {
                        embed = Embed(EmbedType.IMAGE.ordinal, embedUUID, extension)
                        //change to add media
                        (context as MainActivity).requests.setGroupMediaBitmap(
                            embedUUID,
                            groupUID,
                            documentURI,
                            R.mipmap.flantern_logo_foreground
                        )
                    }
                    EmbedType.VIDEO -> {
                        val uuid = "flantern_" + UUID.randomUUID().toString()
                        val out = File.createTempFile(uuid, extension)
                        out.deleteOnExit()
                        GiraffeCompressor.create().input(File(documentURI.path!!)).output(out)
                            .bitRate(1).ready()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                (context as MainActivity).requests.setGroupMediaDocument(
                                    embedUUID,
                                    groupUID,
                                    out.toUri()
                                )
                                embed = Embed(EmbedType.VIDEO.ordinal, embedUUID, extension)
                            }
                    }
                    EmbedType.AUDIO -> {
                        (context as MainActivity).requests.setGroupMediaDocument(
                            embedUUID,
                            groupUID,
                            documentURI
                        )
                        embed = Embed(EmbedType.AUDIO.ordinal, embedUUID, extension)
                    }
                    EmbedType.DOCUMENT -> {
                        (context as MainActivity).requests.setGroupMediaDocument(
                            embedUUID,
                            groupUID,
                            documentURI
                        )
                        embed = Embed(EmbedType.DOCUMENT.ordinal, embedUUID, extension)
                    }
                }
                /*if (context!!.contentResolver.getType(imageURI) == "video/mp4") {
                } else if (context!!.contentResolver.getType(imageURI) == "image/jpeg") {
                    embed = Embed(EmbedType.IMAGE.ordinal, embedUUID)
                    imageURI = Uri.EMPTY
                    (context as MainActivity).requests.setGroupMediaBitmap(
                        embedUUID,
                        groupUID,
                        imageURI
                    )*/
                /*val outputStream = ByteArrayOutputStream()
                BitmapFactory.decodeStream(
                    this.context!!.contentResolver.openInputStream(
                        imageURI
                    )!!
                )
                    .compress(CompressFormat.JPEG, 10, outputStream)
                (this.context as MainActivity).storage.getReference("$groupUID/$embedUUID.jpg")
                    .putBytes(
                        outputStream.toByteArray()
                    )*/
                //}
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
                if (layoutManager.findFirstCompletelyVisibleItemPosition() <= pagedRecycler.pageLength - 1) {
                    pagedRecycler.pageUp()
                } else if (layoutManager.findLastCompletelyVisibleItemPosition() >= pagedRecycler.repo.size - pagedRecycler.pageLength - 1) {
                    pagedRecycler.pageDown()
                }
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
        recyclerView.adapter = adapter
        return binding.root
    }
}