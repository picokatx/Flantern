package com.picobyte.flantern

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.picobyte.flantern.adapters.ChatAdapter
import com.picobyte.flantern.types.Message
import com.picobyte.flantern.adapters.ChatsAdapter
import com.picobyte.flantern.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentChatBinding = FragmentChatBinding.inflate(inflater, container, false)
        val recyclerView = binding.recycler
        val layoutManager = GridLayoutManager(requireActivity(), 1)
        recyclerView.layoutManager = layoutManager
        val messagesUID: ArrayList<String> = ArrayList<String>()
        val groupUID: String = arguments?.getString("group_uid")!!
        val adapter = ChatAdapter(groupUID, messagesUID)
        val ref = (requireActivity() as MainActivity).rtDatabase.getReference("/group_messages/$groupUID")
        binding.testSend.setOnClickListener {
            val message = Message(Firebase.auth.uid!!, binding.editText.text.toString(),0, null, false)
            ref.child(ref.push().key!!).setValue(message)
            (requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID/recent").setValue(message)
            binding.recycler.scrollToPosition(messagesUID.size);
            binding.editText.setText("")
        }
        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(Message::class.java)
                messagesUID.add(snapshot.key!!)
                Log.e("Flantern", "onChildAdded")
                adapter.notifyItemInserted(messagesUID.size-1)
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

        })

        recyclerView.adapter = adapter
        return binding.root
    }
}