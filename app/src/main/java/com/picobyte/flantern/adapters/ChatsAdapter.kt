package com.picobyte.flantern.adapters

import android.os.Bundle
import android.util.Log
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
import com.picobyte.flantern.types.Message
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateWithBundle

class ChatsAdapter(val groups_UID: ArrayList<Pair<Pair<String,String>, Group>>) :
    RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_chats, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(groups_UID[position].second, groups_UID[position].first.second)
        /*val ref = (holder.binding.root.context as MainActivity).rtDatabase.getReference("/groups/${groups_UID[position]}/static")
        Log.e("Flantenr", ref.toString())
        ref.get().addOnCompleteListener {
            holder.bindItems(it.result.getValue(Group::class.java)!!, ref.key!!)
        }*/
    }

    override fun getItemCount() = groups_UID.size
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardChatsBinding.bind(itemView)
        fun bindItems(chp: Group, groupUID: String) {
            binding.root.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("group_uid", groupUID)
                navigateWithBundle(binding.root, R.id.action_global_ChatFragment, bundle)
            }
            binding.chatName.text = chp.name
            binding.chatRecent.text = "${chp.recent?.user}: ${chp.recent?.content}"
            binding.chatRecentDate.text = chp.recent?.timestamp.toString()
        }
    }
}