package com.picobyte.flantern

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.adapters.ChatAdapter
import com.picobyte.flantern.adapters.UserAdapter
import com.picobyte.flantern.databinding.FragmentChatDetailsBinding
import com.picobyte.flantern.types.*
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateUp
import com.picobyte.flantern.utils.navigateWithBundle
import com.picobyte.flantern.wrappers.FullLoadRecyclerWrapper

class ChatDetailsFragment : Fragment() {
    lateinit var pagedRecycler: FullLoadRecyclerWrapper<User>
    lateinit var groupName: String
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentChatDetailsBinding.inflate(inflater, container, false)
        val groupUID: String = arguments?.getString("group_uid")!!
        val newContacts = arguments?.getStringArrayList("user_contacts")
        if (newContacts!=null) {
            (context as MainActivity).requests.addUsersToGroup(groupUID, newContacts)
        }
        //val groupUsersRef =
        //    (requireActivity() as MainActivity).rtDatabase.getReference("/group_users/$groupUID/has")
        /*if (newContacts.isNotEmpty()) {
            for (i in newContacts) {
                val groupUsersKey = groupUsersRef.child("static").push().key
                groupUsersRef.child("static/$groupUsersKey").setValue(i)
                groupUsersRef.child("live/$groupUsersKey").setValue(DatabaseOp.ADD)
                val userRef =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/user_groups/$i/has")
                val userGroupsKey = groupUsersRef.child("static").push().key
                userRef.child("static/$userGroupsKey").setValue(groupUID)
                userRef.child("live/$userGroupsKey").setValue(DatabaseOp.ADD)
            }
        }*/
        (context as MainActivity).requests.getGroup(groupUID, {
            groupName = it.name!!
            binding.topBarTitle.text = it.name
            binding.topBarDescription.text = it.description
            binding.topBarCreated.text = "Created: ${getDate(it.created!!, "dd MMM yy, HH:mm:ss")}"
            (context as MainActivity).requests.getMemberCount(groupUID) { count->
                binding.topBarMembers.text = "$count members"
            }
            if (it.profile != null) {
                (context as MainActivity).requests.getGroupMediaBitmap(it.profile, groupUID, { bitmap ->
                    binding.topBarIcon.setImageBitmap(bitmap)
                })
            } else {
                binding.topBarIcon.setImageResource(R.mipmap.flantern_logo_foreground)
            }
            val usersUID: ArrayList<Pair<Pair<String, String>, User>> =
                ArrayList<Pair<Pair<String, String>, User>>()
            val adapter: UserAdapter = UserAdapter(usersUID)
            adapter.groupUID = groupUID
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
            (context as MainActivity).rtDatabase.getReference("group_messages/$groupUID/live").get().addOnCompleteListener { poke ->
                binding.messageGraphTitle.text = "${poke.result.childrenCount} edits"
            }
            binding.messageGraphTitle.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("group_uid", groupUID)
                navigateWithBundle(binding.root, R.id.action_global_MessageGraphFragment, bundle)
            }
            binding.backBtn.setOnClickListener {
                navigateUp(binding.root)
            }
            binding.membersBarAdd.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt("type", RecyclableType.CONTACTS.ordinal)
                bundle.putString(
                    "title_text",
                    (context as MainActivity).authGoogle.getDisplayName()
                )
                bundle.putString("subtitle_text", "Add Group Members")
                bundle.putString("user_uid", (context as MainActivity).authGoogle.getUID())
                bundle.putInt("destination", R.id.action_global_ChatDetailsFragment)
                val destBundle = Bundle()
                destBundle.putString("group_uid", groupUID)
                bundle.putBundle("destination_bundle", destBundle)
                navigateWithBundle(binding.root, R.id.action_global_ItemSelectFragment, bundle)
            }
            binding.membersBarInvite.setOnClickListener {
                (context as MainActivity).requests.createGroupInvite(groupUID) { code ->
                    Toast.makeText(context, "Your Invite Code is $code", Toast.LENGTH_LONG).show()
                    val clipboard = (context as MainActivity).getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", code))

                }
            }
            (context as MainActivity).requests.getPinnedMessage(groupUID, { msgUID, message ->
                val vHolder = ChatAdapter.ViewHolder(binding.pinnedMessage.root)
                vHolder.bindItems(groupUID, msgUID, message, container!!)
            }, { err ->
                Toast.makeText(
                    context,
                    err,
                    Toast.LENGTH_LONG
                ).show()
            })
            binding.topBarEdit.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("group_uid", groupUID)
                navigateWithBundle(binding.root, R.id.action_global_EditGroupFragment, bundle)
            }

            binding.actionBarExit.setOnClickListener {
                val builder = AlertDialog.Builder(context!!)
                builder.setMessage("Are you sure you want to exit the group")
                    .setPositiveButton("Stay",
                        DialogInterface.OnClickListener { dialog, id ->
                            //Toast.makeText(context, "")
                        })
                    .setNegativeButton("Leave",
                        DialogInterface.OnClickListener { dialog, id ->
                            (context as MainActivity).requests.kickUser((context as MainActivity).authGoogle.getUID(), groupUID) {
                                navigateTo(binding.root, R.id.action_global_HomeFragment)
                                Toast.makeText(
                                    context,
                                    "You left $groupName",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        })
                builder.create().show()
            }
            binding.actionBarDelete.setOnClickListener {
                val builder = AlertDialog.Builder(context!!)
                builder.setMessage("Are you sure you want to delete the group")
                    .setPositiveButton("Yes",
                        DialogInterface.OnClickListener { dialog, id ->
                            (context as MainActivity).requests.deleteGroup(groupUID, {
                                Toast.makeText(
                                    context,
                                    "You deleted $groupName",
                                    Toast.LENGTH_LONG
                                ).show()
                                navigateTo(binding.root, R.id.action_global_HomeFragment)
                            }, { err ->
                                Toast.makeText(
                                    context,
                                    err,
                                    Toast.LENGTH_LONG
                                ).show()
                            })
                        })
                    .setNegativeButton("No",
                        DialogInterface.OnClickListener { dialog, id ->
                        })
                builder.create().show()
            }
        }, {
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_LONG
            ).show()
        })
        /*val groupRef =
            (requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID/static")
        groupRef.get().addOnCompleteListener {
            val group = it.result.getValue(Group::class.java)!!
            groupName = group.name!!
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
            val usersUID: ArrayList<Pair<Pair<String, String>, User>> =
                ArrayList<Pair<Pair<String, String>, User>>()
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
                val bundle = Bundle()
                bundle.putInt("type", RecyclableType.CONTACTS.ordinal)
                bundle.putString(
                    "title_text",
                    (context as MainActivity).authGoogle.getDisplayName()
                )
                bundle.putString("subtitle_text", "Add Group Members")
                bundle.putString("user_uid", (context as MainActivity).authGoogle.getUID())
                bundle.putInt("destination", R.id.action_global_ChatFragment)
                val destBundle = Bundle()
                destBundle.putString("group_uid", groupUID)
                bundle.putBundle("destination_bundle", destBundle)
                navigateWithBundle(binding.root, R.id.action_global_ItemSelectFragment, bundle)
            }
            binding.membersBarInvite.setOnClickListener {
                val code = (requireActivity() as MainActivity).adjectives.random() +
                        (requireActivity() as MainActivity).animals.random()
                (requireActivity() as MainActivity).rtDatabase.getReference("/group_invites/$code")
                    .setValue(groupUID)
            }
            binding.actionBarExit.setOnClickListener {
                val builder = AlertDialog.Builder(context!!)
                builder.setMessage("Are you sure you want to exit the group")
                    .setPositiveButton("Stay",
                        DialogInterface.OnClickListener { dialog, id ->
                            //Toast.makeText(context, "")
                        })
                    .setNegativeButton("Leave",
                        DialogInterface.OnClickListener { dialog, id ->
                            val uid = (requireActivity() as MainActivity).authGoogle.getUID()
                            groupUsersRef.child("static").equalTo(uid).get()
                                .addOnCompleteListener { user ->
                                    for (i in user.result.children) {
                                        groupUsersRef.child("static/${i.key}").removeValue()
                                        groupUsersRef.child("live/${i.key}")
                                            .setValue(DatabaseOp.DELETE)
                                    }
                                }
                            (requireActivity() as MainActivity).rtDatabase.getReference("/user_groups/$uid/has/static")
                                .equalTo(groupUID).get().addOnCompleteListener { user ->
                                    var testNum = 0
                                    for (i in user.result.children) {
                                        testNum++
                                        groupUsersRef.child("live/${i.key}")
                                            .setValue(DatabaseOp.DELETE)
                                        i.ref.removeValue()
                                    }
                                    if (testNum > 0) {
                                        Toast.makeText(
                                            context,
                                            "You left $groupName",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        })
                builder.create().show()
            }
        }*/
        return binding.root
    }
}