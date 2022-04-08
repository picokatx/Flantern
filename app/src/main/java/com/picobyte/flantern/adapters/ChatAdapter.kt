package com.picobyte.flantern.adapters

import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.os.Environment
import android.util.Log
import android.widget.MediaController
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.R
import com.picobyte.flantern.databinding.CardMessageBinding
import com.picobyte.flantern.types.*
import com.picobyte.flantern.utils.ONE_MEGABYTE
import android.content.Context.DOWNLOAD_SERVICE

import androidx.core.content.ContextCompat.getSystemService

import android.app.DownloadManager
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.view.ContextMenu.ContextMenuInfo

import android.view.ContextMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.picobyte.flantern.databinding.DialogInviteCodeBinding
import com.picobyte.flantern.databinding.DialogUserProfileBinding


class ChatAdapter(
    private val groupUID: String,
    private val messagesUID: ArrayList<Pair<String, Message>>,
    private val container: ViewGroup
) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_message, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(
            groupUID,
            messagesUID[position].first,
            messagesUID[position].second,
            container
        )
    }

    override fun getItemCount(): Int {
        return messagesUID.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardMessageBinding.bind(itemView)
        fun bindItems(groupUID: String, messageUID: String, chp: Message, container: ViewGroup) {
            (itemView.context as AppCompatActivity).registerForContextMenu(binding.root)
            binding.root.setOnCreateContextMenuListener { contextMenu, view, contextMenuInfo ->
                contextMenu.add("Delete").setOnMenuItemClickListener {
                    (itemView.context as MainActivity).requests.removeMessage(
                        groupUID,
                        messageUID,
                        {
                            Toast.makeText(itemView.context, "Deleted Message", Toast.LENGTH_LONG)
                                .show()
                        },
                        {
                            Toast.makeText(itemView.context, it, Toast.LENGTH_LONG).show()
                        })
                    true
                }
                contextMenu.add("Modify").setOnMenuItemClickListener {
                    val builder = AlertDialog.Builder(
                        ContextThemeWrapper(itemView.context, R.style.AlertDialogCustom)
                    )
                    val alertDialogView =
                        DialogInviteCodeBinding.inflate(
                            LayoutInflater.from(itemView.context),
                            container,
                            false
                        )
                    val dialog = builder.setMessage("Edit Message")
                        .setView(alertDialogView.root)
                        .create()
                    alertDialogView.join.setOnClickListener {
                        (itemView.context as MainActivity).requests.modifyMessageContent(
                            groupUID,
                            messageUID,
                            alertDialogView.inviteCodeField.text.toString(), {
                                Toast.makeText(
                                    itemView.context,
                                    "Message Modified",
                                    Toast.LENGTH_LONG
                                ).show()
                                dialog.dismiss()
                            }, {
                                Toast.makeText(
                                    itemView.context,
                                    it,
                                    Toast.LENGTH_LONG
                                ).show()
                                dialog.dismiss()
                            }
                        )
                    }
                    dialog.show()
                    true
                }
                contextMenu.add("View Contact").setOnMenuItemClickListener {
                    val builder = AlertDialog.Builder(
                        ContextThemeWrapper(itemView.context, R.style.AlertDialogCustom)
                    )
                    val alertDialogView =
                        DialogUserProfileBinding.inflate(
                            LayoutInflater.from(itemView.context),
                            container,
                            false
                        )

                    (itemView.context as MainActivity).requests.getUser(chp.user!!, {
                        alertDialogView.userBan.visibility = View.GONE
                        alertDialogView.userKick.visibility = View.GONE
                        alertDialogView.userName.text = it.name
                        alertDialogView.userDescription.text = it.description
                        (itemView.context as MainActivity).requests.getUserProfileBitmap(
                            it.profile!!,
                            { bm ->
                                alertDialogView.userProfile.setImageBitmap(bm)
                            })
                        when (it.status!!) {
                            Status.ACTIVE.ordinal -> {
                                alertDialogView.userStatus.setImageResource(R.drawable.active)
                            }
                            Status.DONOTDISTURB.ordinal -> {
                                alertDialogView.userStatus.setImageResource(R.drawable.do_not_disturb)
                            }
                            Status.IDLE.ordinal -> {
                                alertDialogView.userStatus.setImageResource(R.drawable.idle)
                            }
                            Status.SLEEP.ordinal -> {
                                alertDialogView.userStatus.setImageResource(R.drawable.offline)
                            }
                        }
                        (itemView.context as MainActivity).requests.isAdmin(
                            (itemView.context as MainActivity).authGoogle.getUID(),
                            groupUID
                        ) { isAdmin ->
                            if (isAdmin) {
                                Log.e("Flantern", "isadmin")
                                alertDialogView.userBan.visibility = View.VISIBLE
                                alertDialogView.userBan.setOnClickListener {
                                    (itemView.context as MainActivity).requests.blacklistUser(chp.user, groupUID, {
                                        (itemView.context as MainActivity).requests.kickUser(chp.user, groupUID) {
                                            Toast.makeText(itemView.context, "User has been banned", Toast.LENGTH_LONG).show()
                                        }
                                    })
                                }
                                alertDialogView.userKick.visibility = View.VISIBLE
                                alertDialogView.userKick.setOnClickListener {
                                    (itemView.context as MainActivity).requests.kickUser(chp.user, groupUID) {
                                        Toast.makeText(itemView.context, "User has been banned", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    })
                    builder.setMessage("User Profile")
                        .setView(alertDialogView.root)
                        .create()
                        .show()

                    true
                }
                contextMenu.add("Add Contact").setOnMenuItemClickListener {
                    (itemView.context as MainActivity).requests.addContact(chp.user!!)
                    true
                }
                contextMenu.add("Pin").setOnMenuItemClickListener {
                    (itemView.context as MainActivity).requests.pinMessage(groupUID, chp, {
                        Toast.makeText(
                            itemView.context,
                            "Message Pinned",
                            Toast.LENGTH_LONG
                        ).show()
                    }, {
                        Toast.makeText(
                            itemView.context,
                            it,
                            Toast.LENGTH_LONG
                        ).show()
                    })
                    true
                }
            }

            val userMap = (itemView.context as MainActivity).userMap
            if (!userMap.containsKey(chp.user)) {
                binding.nameField.text = "..."
                (itemView.context as MainActivity).requests.getUser(chp.user!!, {
                    userMap[chp.user] = it
                    binding.nameField.text = userMap[chp.user]!!.name
                }, {
                    Toast.makeText(
                        itemView.context,
                        it,
                        Toast.LENGTH_LONG
                    ).show()
                })
            } else {
                binding.nameField.text = userMap[chp.user]!!.name
            }
            binding.testImage.visibility = View.GONE
            binding.testVideo.visibility = View.GONE
            binding.testDocument.visibility = View.GONE
            binding.embedField.setOnClickListener {
                if (chp.embed != null) {
                    when (chp.embed.type) {
                        EmbedType.IMAGE.ordinal -> {
                            (itemView.context as MainActivity).storage.reference.child("$groupUID/${chp.embed.ref}.jpg")
                                .getBytes(ONE_MEGABYTE).addOnCompleteListener {
                                    Log.e("Flantern", "hello please set the image now")
                                    (itemView.context as MainActivity).runOnUiThread {
                                        binding.testImage.setImageBitmap(
                                            BitmapFactory.decodeByteArray(
                                                it.result,
                                                0,
                                                it.result.size
                                            )
                                        )
                                    }
                                }
                            binding.testImage.visibility = View.VISIBLE
                        }
                        EmbedType.VIDEO.ordinal -> {
                            (itemView.context as MainActivity).requests.getGroupMediaDocumentUri(
                                chp.embed,
                                groupUID
                            ) {
                                val mediaController = MediaController(itemView.context)
                                mediaController.setAnchorView(binding.testVideo)
                                binding.testVideo.setMediaController(mediaController)
                                binding.testVideo.setVideoURI(it)
                                binding.testVideo.visibility = View.VISIBLE
                                binding.testVideo.requestFocus();
                                binding.testVideo.start()
                                mediaController.show(3000)
                            }
                        }
                        EmbedType.AUDIO.ordinal -> {
                            (itemView.context as MainActivity).requests.getGroupMediaDocumentUri(
                                chp.embed,
                                groupUID
                            ) {
                                val mediaController = MediaController(itemView.context)
                                mediaController.setAnchorView(binding.testVideo)
                                binding.testVideo.setMediaController(mediaController)
                                binding.testVideo.setVideoURI(it)
                                binding.testVideo.visibility = View.VISIBLE
                                binding.testVideo.requestFocus();
                                binding.testVideo.start();
                                //mediaController.show(3000)
                            }
                        }
                        EmbedType.DOCUMENT.ordinal -> {
                            binding.testDocument.visibility = View.VISIBLE
                            binding.testDocument.setOnClickListener {
                                (itemView.context as MainActivity).requests.getGroupMediaDocumentUri(
                                    chp.embed,
                                    groupUID
                                ) {
                                    val request = DownloadManager.Request(it)
                                    request.setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        "fileName"
                                    )
                                    //https://stackoverflow.com/questions/28183893/how-to-store-files-generated-from-app-in-downloads-folder-of-android
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    val manager =
                                        itemView.context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager?
                                    manager!!.enqueue(request)
                                }
                            }
                        }
                    }
                }
            }
            Log.e("Flantern", chp.content + chp.embed)
            if (chp.embed != null) {
                binding.embedField.visibility = View.VISIBLE
                when (chp.embed.type) {
                    EmbedType.IMAGE.ordinal -> {
                        binding.embedField.setImageResource(R.drawable.image_frame)
                    }
                    EmbedType.VIDEO.ordinal -> {
                        binding.embedField.setImageResource(R.drawable.video)
                    }
                    EmbedType.AUDIO.ordinal -> {
                        binding.embedField.setImageResource(R.drawable.audio)
                    }
                    EmbedType.DOCUMENT.ordinal -> {
                        binding.embedField.setImageResource(R.drawable.document)
                    }
                }
            } else {
                binding.embedField.visibility = View.GONE
            }
            /*var test: Bitmap
            if (chp.embed != null) {
                when (chp.embed.type) {
                    EmbedType.IMAGE.ordinal -> {
                        (itemView.context as MainActivity).storage.reference.child("$groupUID/${chp.embed.ref}.jpg")
                            .downloadUrl.addOnCompleteListener {
                                Picasso.get().load(it.result).into(binding.imageField)
                            }
                        /*(itemView.context as MainActivity).runOnUiThread {
                                    binding.imageField.setImageBitmap(BitmapFactory.decodeStream(stream))
                                }*/
                    }
                }
            } else {
                binding.imageField.setImageResource(R.mipmap.flantern_logo_foreground)
            }*/
            binding.contentField.text = chp.content
            binding.timestampField.text = getDate(chp.timestamp!!, "hh:mm:ss")
        }
    }
}

/*class ChatAdapter(private val options: DatabasePagingOptions<Message>) :
    FirebaseRecyclerPagingAdapter<
            Message, ChatAdapter.ChatViewHolder>(options) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {

        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_message, parent, false)
        return ChatViewHolder(v)
    }

    /*
    * So change of plan, model will now also include message key, when viewholder is binded, append message key to arraylist
    * onChildAdded,
    */
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int, model: Message) {
        holder.bindItems(model)
    }

    /*override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        /*val msgref =
            (holder.binding.root.context as MainActivity).rtDatabase.getReference("/group_messages/${group}/${messages_UID[position]}")

        val ref =
            (holder.binding.root.context as MainActivity).rtDatabase.getReference("/group_messages/${group}/${messages_UID[position]}")
        ref.get().addOnCompleteListener {
            holder.bindItems(it.result.getValue(Message::class.java)!!)
        }*/
        Log.e("Flantern", messages.toString())
        holder.bindItems(messages[position])
        Log.e("Flantern", "onBindViewHolder")
    }*/
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardMessageBinding.bind(itemView)
        fun bindItems(chp: Message) {
            Log.e("Flantern", chp.content!!)
            binding.nameField.text = chp.user
            binding.contentField.text = chp.content
            binding.timestampField.text = chp.timestamp.toString()
        }
    }

}*/