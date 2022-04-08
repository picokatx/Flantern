package com.picobyte.flantern.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.R
import com.picobyte.flantern.databinding.CardChatsBinding

import com.google.firebase.database.*
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.types.*
import com.picobyte.flantern.utils.navigateWithBundle

class ChatsAdapter(val groups_UID: ArrayList<Pair<Pair<String, String>, Group>>) :
    RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {
    val selected: ArrayList<String> = ArrayList<String>()
    var selectable = SelectableType.NONE
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_chats, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(groups_UID[position].second, groups_UID[position].first.second, selected, selectable)
        /*val ref = (holder.binding.root.context as MainActivity).rtDatabase.getReference("/groups/${groups_UID[position]}/static")
        Log.e("Flantenr", ref.toString())
        ref.get().addOnCompleteListener {
            holder.bindItems(it.result.getValue(Group::class.java)!!, ref.key!!)
        }*/
    }

    override fun getItemCount() = groups_UID.size
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardChatsBinding.bind(itemView)
        fun bindItems(chp: Group, groupUID: String, selected: ArrayList<String>, selectable: SelectableType) {
            if (selectable == SelectableType.MULTI) {
                binding.root.isClickable = true
                binding.root.setOnClickListener {
                    val idx = selected.indexOf(groupUID)
                    if (idx == -1) {
                        selected.add(groupUID)
                    } else {
                        selected.removeAt(idx)
                    }
                }
            } else if (selectable == SelectableType.SINGLE) {
                binding.root.isClickable = true
                binding.root.setOnClickListener {
                    if (selected.size==0) {
                        selected.add(groupUID)
                    } else {
                        selected[0] = groupUID
                    }
                }
            }
            binding.root.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("group_uid", groupUID)
                navigateWithBundle(binding.root, R.id.action_global_ChatFragment, bundle)
            }
            binding.chatName.text = chp.name
            (itemView.context as MainActivity).requests.getRecent(groupUID, { uid, message ->
                (itemView.context as MainActivity).requests.getUser(message.user!!, { user ->
                    binding.chatRecent.text = "${user.name}: ${message.content}"
                    binding.chatRecentDate.text = getDate(message.timestamp!!, "hh:mm:ss")
                })
            }, {
                Toast.makeText(
                    itemView.context,
                    it,
                    Toast.LENGTH_LONG
                ).show()
            })
            (itemView.context as MainActivity).rtDatabase.getReference("group_messages/$groupUID/live")
                .orderByKey().limitToLast(1).addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        if (snapshot.child("op").getValue(Int::class.java)==DatabaseOp.ADD.ordinal) {
                            (itemView.context as MainActivity).requests.getMessage(groupUID, snapshot.key!!, {
                                (itemView.context as MainActivity).requests.getUser(it.user!!, { user ->
                                    binding.chatRecent.text = "${user.name}: ${it.content}"
                                    binding.chatRecentDate.text = getDate(it.timestamp!!, "hh:mm:ss")
                                    (itemView.context as MainActivity).requests.setRecent(groupUID, it, {}, { err ->
                                        Toast.makeText(
                                            itemView.context,
                                            err,
                                            Toast.LENGTH_LONG
                                        ).show()

                                    })
                                }, { err ->
                                    Toast.makeText(
                                        itemView.context,
                                        err,
                                        Toast.LENGTH_LONG
                                    ).show()
                                })
                            }, {
                                Toast.makeText(
                                    itemView.context,
                                    it,
                                    Toast.LENGTH_LONG
                                ).show()
                            })
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
}