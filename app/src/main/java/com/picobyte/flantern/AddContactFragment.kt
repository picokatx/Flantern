package com.picobyte.flantern

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.picobyte.flantern.adapters.UserAdapter
import com.picobyte.flantern.databinding.FragmentAddContactBinding
import com.picobyte.flantern.databinding.FragmentItemSelectBinding
import com.picobyte.flantern.types.DatabaseOp
import com.picobyte.flantern.types.SelectableType
import com.picobyte.flantern.types.User
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateUp
import com.picobyte.flantern.utils.navigateWithBundle
import com.picobyte.flantern.wrappers.PagedRecyclerWrapper

class AddContactFragment : Fragment() {
    val usersUID: ArrayList<Pair<Pair<String, String>, User>> =
        ArrayList<Pair<Pair<String, String>, User>>()

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
        val layoutManager = LinearLayoutManager(context)
        binding.contactRecycler.layoutManager = layoutManager
        binding.contactRecycler.adapter = adapter
        binding.topBarBack.setOnClickListener {
            navigateUp(binding.root)
        }
        binding.search.setOnClickListener {
            if (binding.contactNameField.text.length < 4) {
                Toast.makeText(context, "Search using at least 4 characters!", Toast.LENGTH_LONG)
                    .show()
            } else {
                (context as MainActivity).requests.getUsersByName(
                    binding.contactNameField.text.toString(),
                    {
                        adapter.notifyDataSetChanged()
                    },
                    { key, user ->
                        usersUID.add(
                            Pair(Pair("", key), user)
                        )
                    }
                )
                /*val lower = binding.contactNameField.text.toString()
                val upper = binding.contactNameField.text.toString()
                    .substring(
                        0,
                        lower.length - 1
                    ) + binding.contactNameField.text[lower.length - 1].inc()
                Log.e("Flantern", lower)
                Log.e("Flantern", upper)

                usersUID.clear()
                (requireActivity() as MainActivity).rtDatabase.getReference("/user")
                    .orderByChild("static/name").startAt(lower).endBefore(upper).get()
                    .addOnCompleteListener {
                        it.result.children.forEach { user ->
                            Log.e("Flantern", user.child("static").ref.toString())
                            usersUID.add(
                                Pair(
                                    Pair("", user.key!!),
                                    user.child("static").getValue(User::class.java)!!
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                    }*/
            }
        }

        binding.proceed.setOnClickListener {
            if (adapter.selected.size == 0) {
                Toast.makeText(context, "Select a contact first!", Toast.LENGTH_LONG)
                    .show()
            } else {

                val uid = (requireActivity() as MainActivity).authGoogle.getUID()
                val ref =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/user_contacts/$uid/has")
                val key = ref.child("static").push().key
                ref.child("static").equalTo(adapter.selected[0]).get().addOnCompleteListener {
                    if (it.result.exists()) {
                        Toast.makeText(
                            context,
                            "You've already added this contact!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        ref.child("static/$key").setValue(adapter.selected[0])
                        ref.child("live/${key}/op").setValue(DatabaseOp.ADD.ordinal)
                        if (adapter.selected[0] != uid) {
                            val otherRef =
                                (requireActivity() as MainActivity).rtDatabase.getReference("/user_contacts/${adapter.selected[0]}/has")
                            val otherKey = otherRef.child("static").push().key
                            otherRef.child("static/$otherKey").setValue(uid)
                            otherRef.child("live/$otherKey/op").setValue(DatabaseOp.ADD.ordinal)
                        }
                        val bundle = Bundle()
                        bundle.putString("contact_uid", adapter.selected[0])
                        navigateWithBundle(binding.root, R.id.action_global_HomeFragment, bundle)
                    }
                }
                /*ef.child("static").get().addOnCompleteListener {
                    if (it.result.hasChild(adapter.selected[0])) {
                        Toast.makeText(
                            context,
                            "You've already added this contact!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        ref.child("static/${adapter.selected[0]}").setValue(key)
                        ref.child("live/${key}").setValue(adapter.selected[0])
                        if (adapter.selected[0] != uid) {
                            val otherRef =
                                (requireActivity() as MainActivity).rtDatabase.getReference("/user_contacts/${adapter.selected[0]}/has")
                            val otherKey = otherRef.child("static").push().key
                            otherRef.child("static/$uid").setValue(otherKey)
                            otherRef.child("live/$otherKey").setValue(uid)
                        }
                        val bundle = Bundle()
                        bundle.putString("contact_uid", adapter.selected[0])
                        navigateWithBundle(binding.root, R.id.action_global_HomeFragment, bundle)
                    }
                }*/
            }
        }
        return binding.root
    }
}