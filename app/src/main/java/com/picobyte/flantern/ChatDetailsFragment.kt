package com.picobyte.flantern

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.picobyte.flantern.adapters.UserAdapter
import com.picobyte.flantern.databinding.FragmentChatDetailsBinding
import com.picobyte.flantern.types.Group
import com.picobyte.flantern.types.User
import com.picobyte.flantern.types.getDate
import com.picobyte.flantern.utils.ONE_MEGABYTE
import com.picobyte.flantern.wrappers.FullLoadRecyclerWrapper
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper

class ChatDetailsFragment : Fragment() {
    lateinit var pagedRecycler: FullLoadRecyclerWrapper<User>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentChatDetailsBinding.inflate(inflater, container, false)
        val groupUID: String = arguments?.getString("group_uid")!!
        val groupRef =
            (requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID/static")
        groupRef.get().addOnCompleteListener {
            val group = it.result.getValue(Group::class.java)!!
            binding.topBarTitle.text = group.name
            binding.topBarDescription.text = group.description
            binding.topBarCreated.text = getDate(group.created!!, "dd/MM/yy")
            if (group.profile != null) {
                (context as MainActivity).storage.getReference("$groupUID/${group.profile}.jpg")
                    .getBytes(ONE_MEGABYTE).addOnCompleteListener { image ->
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
            val usersUID: ArrayList<Pair<Pair<String, String>, User>> = ArrayList<Pair<Pair<String, String>, User>>()
            val adapter: UserAdapter = UserAdapter(usersUID)
            val layoutManager = LinearLayoutManager(requireActivity())
            binding.membersBarContent.layoutManager = layoutManager
            binding.membersBarContent.adapter = adapter
            val ref =
                (requireActivity() as MainActivity).rtDatabase.getReference("/group_users/$groupUID/has")
            val userRef =
                (requireActivity() as MainActivity).rtDatabase.getReference("/user")
            pagedRecycler = FullLoadRecyclerWrapper<User>(
                adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
                binding.membersBarContent,
                ref,
                userRef,
                User::class.java,
                usersUID,
                16
            )
            pagedRecycler.initializePager()
            pagedRecycler.addItemListener()
            binding.membersBarAdd.setOnClickListener {
                //todo: add member select fragment (and arbitrary item select fragment)
            }
            binding.actionBarExit.setOnClickListener {
                //todo: add way to commit quit
            }
        }

        return binding.root
    }
}