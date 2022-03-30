package com.picobyte.flantern

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.picobyte.flantern.adapters.UserAdapter
import com.picobyte.flantern.databinding.FragmentAddContactBinding
import com.picobyte.flantern.databinding.FragmentItemSelectBinding
import com.picobyte.flantern.types.SelectableType
import com.picobyte.flantern.types.User
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateWithBundle
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper

class AddContactFragment : Fragment() {
    val usersUID: ArrayList<Pair<Pair<String, String>, User>> = ArrayList<Pair<Pair<String, String>, User>>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAddContactBinding.inflate(inflater, container, false)
        binding.topBarBack.setOnClickListener {
            navigateTo(binding.root, R.id.action_global_HomeFragment)
        }
        val adapter: UserAdapter = UserAdapter(usersUID)
        adapter.selectable = SelectableType.SINGLE
        binding.search.setOnClickListener {
            if (binding.contactNameField.text.length < 4) {
                Toast.makeText(context, "Search using at least 4 characters!", Toast.LENGTH_LONG)
                    .show()
            } else {
                val lower = binding.contactNameField.text.toString()
                val upper = binding.contactNameField.text.toString()
                    .substring(0, lower.length - 2) + binding.contactNameField.text[lower.length].inc()
                usersUID.clear()
                (requireActivity() as MainActivity).rtDatabase.getReference("/user")
                    .orderByChild("name").startAt(lower).endBefore(upper).get().addOnCompleteListener {
                        it.result.children.forEach { user ->
                            usersUID.add(Pair(Pair("",user.key!!), user.getValue(User::class.java)!!))
                        }
                    }
                adapter.notifyDataSetChanged()
            }
        }

        binding.proceed.setOnClickListener {
            if (adapter.selected.size==0) {
                Toast.makeText(context, "Select a contact first!", Toast.LENGTH_LONG)
                    .show()
            } else {
                val ref = (requireActivity() as MainActivity).rtDatabase.getReference("/user_contacts/${(requireActivity() as MainActivity).authGoogle.getUID()}/has/static")
                ref.child(adapter.selected[0]).setValue(true)
                val bundle = Bundle()
                bundle.putString("contact_uid", adapter.selected[0])
                navigateWithBundle(binding.root, R.id.action_global_HomeFragment, bundle)
            }
        }
        return binding.root
    }
}