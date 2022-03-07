package com.picobyte.flantern.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.R
import com.picobyte.flantern.databinding.CardChatsBinding
import com.picobyte.flantern.types.Group
import com.google.firebase.database.DataSnapshot

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.db.GroupsViewModel

class ChatsAdapter(val groups_UID: ArrayList<String>) :
    RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_chats, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.binding.root.context as MainActivity).rtDatabase.getReference("/groups/${groups_UID[position]}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)
                    if (group != null) {
                        holder.bindItems(group)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    return
                }
            })
    }

    override fun getItemCount() = groups_UID.size
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardChatsBinding.bind(itemView)
        fun bindItems(chp: Group) {
            binding.chatName.text = chp.name
            binding.chatRecent.text = "${chp.recent?.user}: ${chp.recent?.content}"
            binding.chatRecentDate.text = chp.recent?.timestamp.toString()
        }
    }
}