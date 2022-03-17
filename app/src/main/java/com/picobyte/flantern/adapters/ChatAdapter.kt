package com.picobyte.flantern.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.R
import com.picobyte.flantern.databinding.CardMessageBinding
import com.picobyte.flantern.types.Message
import com.picobyte.flantern.types.User
import com.picobyte.flantern.types.getDate

class ChatAdapter(private val messagesUID: ArrayList<Pair<String, Message>>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_message, parent, false)
        return ViewHolder(v)
    }

     override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(messagesUID[position].second)
    }

    override fun getItemCount(): Int {
        return messagesUID.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardMessageBinding.bind(itemView)
        fun bindItems(chp: Message) {
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