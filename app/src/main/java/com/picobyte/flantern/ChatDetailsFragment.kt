package com.picobyte.flantern

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.adapters.UserAdapter
import com.picobyte.flantern.databinding.FragmentChatDetailsBinding
import com.picobyte.flantern.types.Group
import com.picobyte.flantern.types.User
import com.picobyte.flantern.types.getDate
import com.picobyte.flantern.utils.ONE_MEGABYTE
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper

class ChatDetailsFragment : Fragment() {
    lateinit var pagedRecycler: PagedRecyclerWrapper<User>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentChatDetailsBinding.inflate(inflater, container, false)
        val groupUID: String = arguments?.getString("group_uid")!!
        val groupRef =
            (requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID")
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
            val messagesUID: ArrayList<Pair<String, User>> = ArrayList<Pair<String, User>>()
            val adapter: UserAdapter = UserAdapter(groupUID, messagesUID)
            val ref =
                (requireActivity() as MainActivity).rtDatabase.getReference("/group_messages/$groupUID")
            pagedRecycler = PagedRecyclerWrapper<User>(
                groupUID,
                adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
                binding.membersBarContent,
                ref,
                User::class.java,
                messagesUID,
                16
            )
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