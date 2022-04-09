package com.picobyte.flantern

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.picobyte.flantern.databinding.FragmentEditGroupBinding
import com.picobyte.flantern.types.Group
import com.picobyte.flantern.types.GroupEdit
import com.picobyte.flantern.utils.navigateTo
import com.picobyte.flantern.utils.navigateUp
import java.io.ByteArrayOutputStream
import java.util.*

class EditGroupFragment : Fragment() {
    var imageURI: Uri = Uri.EMPTY
    var imageWasChanged = false
    lateinit var groupProfileField: ImageView
    val galleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                imageURI = result.data!!.data!!
                groupProfileField.setImageBitmap(
                    BitmapFactory.decodeStream(
                        this.context!!.contentResolver.openInputStream(
                            imageURI
                        )!!
                    )
                )
                imageWasChanged = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditGroupBinding.inflate(inflater, container, false)
        groupProfileField = binding.groupProfileField
        val contacts = arguments?.getStringArrayList("user_contacts")!!
        val groupUID = arguments?.getString("group_uid")
        if (groupUID != null) {
            (context as MainActivity).requests.getGroup(groupUID, {
                binding.groupNameField.setText(it.name)
                binding.groupDescField.setText(it.description)
                if (it.profile != null) {
                    (context as MainActivity).requests.getGroupMediaBitmap(
                        it.profile,
                        groupUID,
                        { bitmap ->
                            binding.groupProfileField.setImageBitmap(bitmap)
                        })
                }
            }, {
                Toast.makeText(
                    context,
                    it,
                    Toast.LENGTH_LONG
                ).show()
            })
            /*(requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID/static")
                .get().addOnCompleteListener {
                    val group = it.result.getValue(Group::class.java)!!
                    binding.groupNameField.setText(group.name)
                    binding.groupDescField.setText(group.description)
                    if (group.profile != null) {
                        (context as MainActivity).storage.getReference("$groupUID/${group.profile}.jpg")
                            .getBytes(1000 * 1000).addOnCompleteListener { image ->
                                binding.groupProfileField.setImageBitmap(
                                    BitmapFactory.decodeByteArray(
                                        image.result,
                                        0,
                                        image.result.size
                                    )
                                )
                            }
                    }
                }*/
        }
        binding.groupProfileField.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                )
                galleryLauncher.launch(intent)
            }
        }
        binding.topBarBack.setOnClickListener {
            navigateUp(binding.root)
        }
        binding.proceed.setOnClickListener {
            //todo: create/edit a group here
            Log.e("Flantern", "hello click")
            if (groupUID == null) {
                var embedUUID: String? = null
                if (imageURI != Uri.EMPTY) {
                    embedUUID = UUID.randomUUID().toString()
                }
                val group = Group(
                    binding.groupNameField.text.toString(),
                    binding.groupDescField.text.toString(),
                    embedUUID,
                    null,
                    System.currentTimeMillis()
                )
                Log.e("Flantern", "hello initialize rpoced")
                (context as MainActivity).requests.createGroup(group, contacts) {
                    (context as MainActivity).requests.setGroupMediaBitmap(
                        embedUUID,
                        it,
                        imageURI,
                        R.mipmap.flantern_logo_foreground
                    ) {
                    }
                    navigateTo(binding.root, R.id.action_global_HomeFragment)
                }

                /*val ref =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/groups").push()
                val outputStream = ByteArrayOutputStream()
                if (imageURI != Uri.EMPTY) {
                    embedUUID = UUID.randomUUID().toString()
                    BitmapFactory.decodeStream(
                        this.context!!.contentResolver.openInputStream(
                            imageURI
                        )!!
                    ).compress(Bitmap.CompressFormat.JPEG, 10, outputStream)
                    (this.context as MainActivity).storage.getReference("${ref.key}/$embedUUID.jpg")
                        .putBytes(
                            outputStream.toByteArray()
                        )
                }
                ref.child("static").setValue(group)
                ref.child("live").push().setValue(GroupEdit.CREATED.ordinal)
                contacts.forEach {
                    val groupUserRef =
                        (requireActivity() as MainActivity).rtDatabase.getReference("/group_users/${ref.key}/has")
                    val staticRef = groupUserRef.child("static").push()
                    staticRef.setValue(it)
                    groupUserRef.child("live/${staticRef.key}").setValue(DatabaseOp.ADD.ordinal)
                    val userGroupsRef =
                        (requireActivity() as MainActivity).rtDatabase.getReference("/user_groups/${it}/has")
                    val groupStaticRef = userGroupsRef.child("static").push()
                    groupStaticRef.setValue(ref.key)
                    userGroupsRef.child("live/${groupStaticRef.key}")
                        .setValue(DatabaseOp.ADD.ordinal)
                }
                val msgRef =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/group_messages/${ref.key}/static")
                        .push()
                msgRef.setValue(
                    Message(
                        "Flantern",
                        "You've created a group!",
                        System.currentTimeMillis()
                    )
                )
                (requireActivity() as MainActivity).rtDatabase.getReference("/group_messages/${ref.key}/live/${msgRef.key}")
                    .setValue(DatabaseOp.ADD)*/
            } else {
                val ref =
                    (requireActivity() as MainActivity).rtDatabase.getReference("/groups/$groupUID/static")
                        .push()
                ref.get().addOnCompleteListener {
                    val group = it.result.getValue(Group::class.java)!!
                    if (group.name != binding.groupNameField.text.toString()) {
                        Log.e("Flantern", "hello edit group am triggering")
                        ref.child("name").setValue(binding.groupNameField.text.toString())
                        ref.child("live").push().child("op").setValue(GroupEdit.NAME.ordinal)
                    }
                    if (group.description != binding.groupDescField.text.toString()) {
                        ref.child("description").setValue(binding.groupDescField.text.toString())
                        ref.child("live").push().child("op").setValue(GroupEdit.DESCRIPTION.ordinal)
                    }
                    if (imageWasChanged) {
                        val embedUUID = UUID.randomUUID().toString()
                        val outputStream = ByteArrayOutputStream()
                        Log.e("Flantern", "hello edit group am triggering")
                        BitmapFactory.decodeStream(
                            this.context!!.contentResolver.openInputStream(
                                imageURI
                            )!!
                        ).compress(Bitmap.CompressFormat.JPEG, 10, outputStream)
                        (this.context as MainActivity).storage.getReference("$groupUID/$embedUUID.jpg")
                            .putBytes(
                                outputStream.toByteArray()
                            )
                        ref.child("profile").setValue(embedUUID)
                        ref.child("live").push().child("op").setValue(GroupEdit.PROFILE.ordinal)
                    }
                    Log.e("Flantern", "hello edit group am triggering")
                    navigateTo(binding.root, R.id.action_global_HomeFragment)
                }

            }

        }
        return binding.root
    }
}