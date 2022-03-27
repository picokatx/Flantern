package com.picobyte.flantern.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.R
import com.picobyte.flantern.databinding.CardMessageBinding
import com.picobyte.flantern.types.*
import com.squareup.picasso.Picasso
import java.io.InputStream
import androidx.constraintlayout.widget.ConstraintSet




const val ONE_MEGABYTE: Long = 1000*1000
class ChatAdapter(
    private val groupUID: String,
    private val messagesUID: ArrayList<Pair<String, Message>>
) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_message, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(groupUID, messagesUID[position].second)
    }

    override fun getItemCount(): Int {
        return messagesUID.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardMessageBinding.bind(itemView)
        fun bindItems(groupUID: String, chp: Message) {
            val userMap = (itemView.context as MainActivity).userMap
            if (!userMap.containsKey(chp.user)) {
                binding.nameField.text = "..."
                (itemView.context as MainActivity).rtDatabase.getReference("/user/${chp.user}")
                    .get().addOnCompleteListener {
                        userMap[chp.user!!] = it.result.getValue(User::class.java)!!
                        binding.nameField.text = userMap[chp.user]!!.name
                    }
            } else {
                binding.nameField.text = userMap[chp.user]!!.name
            }
            binding.testImage.visibility = View.GONE
            binding.embedField.setOnClickListener {
                if (chp.embed != null) {
                    when (chp.embed.type) {
                        EmbedType.IMAGE.ordinal -> {
                            (itemView.context as MainActivity).storage.reference.child("$groupUID/${chp.embed.ref}.jpg")
                                .getBytes(ONE_MEGABYTE).addOnCompleteListener {
                                    Log.e("Flantern", "hello please set the image now")
                                    (itemView.context as MainActivity).runOnUiThread {
                                        binding.testImage.setImageBitmap(
                                            BitmapFactory.decodeByteArray(it.result, 0, it.result.size)
                                        )
                                    }
                                }
                            binding.testImage.visibility = View.VISIBLE
                        }
                        EmbedType.VIDEO.ordinal -> {
                        }
                        EmbedType.AUDIO.ordinal -> {
                        }
                        EmbedType.DOCUMENT.ordinal -> {
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