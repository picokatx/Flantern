package com.picobyte.flantern.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.types.*
import com.picobyte.flantern.utils.ONE_MEGABYTE
import com.picobyte.flantern.utils.getMimeType
import java.io.ByteArrayOutputStream
import java.util.*

class FlanternRequests(
    val context: Context,
    val database: FirebaseDatabase,
    val storage: FirebaseStorage,
    val auth: FirebaseAuth
) {
    fun addMessage(item: Message) {
        val ref = database.getReference("group_messages")
        val key = ref.child("static").push().key!!
        ref.child("static/$key").setValue(item)
        ref.child("live/$key/op").setValue(DatabaseOp.ADD.ordinal)
    }

    fun removeMessage(key: String) {
        val ref = database.getReference("group_messages")
        ref.child("static/$key/user").get().addOnCompleteListener {
            if (it.result.exists()) {
                ref.child("static/$key").removeValue()
                ref.child("live/$key/op").setValue(DatabaseOp.DELETE.ordinal)
                ref.child("live/$key/data").setValue(it.result.getValue(String::class.java))
            }
        }
    }

    fun modifyMessage(key: String, item: Message) {
        val ref = database.getReference("group_messages")
        ref.child("static").child(key).setValue(item)
        ref.child("live/$key/op").setValue(DatabaseOp.MODIFY.ordinal)
    }

    fun getMessage(
        groupUID: String,
        key: String,
        success: (msg: Message) -> Unit = {},
        error: () -> Unit = {}
    ) {
        database.getReference("group_messages/$groupUID/static/$key").get().addOnCompleteListener {
            if (it.result.exists()) {
                val msg = it.result.getValue(Message::class.java)!!
                success(msg)
            } else {
                error()
            }
        }
    }

    fun setRecent(groupUID: String, msg: Message, callback: () -> Unit = {}) {
        database.getReference("groups/$groupUID/static/recent").setValue(msg)
            .addOnCompleteListener {
                callback()
            }
    }

    fun getLoggedInUser(success: (user: User) -> Unit = {}, error: () -> Unit = {}) {
        //Jsoup.connect("https://jsoup.org/cookbook/extracting-data/attributes-text-html")
        database.getReference("/user/${auth.currentUser?.uid}").get().addOnCompleteListener {
            if (it.result.exists()) {
                success(it.result.getValue(User::class.java)!!)
            } else {
                error()
            }
        }
    }

    fun leaveGroup(groupUID: String, callback: () -> Unit = {}) {
        val groupUsersRef = database.getReference("/group_users/$groupUID/has")
        groupUsersRef.child("static").equalTo(auth.currentUser?.uid).get()
            .addOnCompleteListener { user ->
                if (user.result.hasChildren()) {
                    val groupUserEntry = user.result.children.first()
                    groupUsersRef.child("static/${groupUserEntry.key}").removeValue()
                    groupUsersRef.child("live/${groupUserEntry.key}/op").setValue(DatabaseOp.DELETE)
                    groupUsersRef.child("live/${groupUserEntry.key}/data")
                        .setValue(auth.currentUser?.uid)
                        .addOnCompleteListener {
                            database.getReference("/user_groups/${auth.currentUser?.uid}/has/static")
                                .equalTo(groupUID).get().addOnCompleteListener { user ->
                                    if (user.result.hasChildren()) {
                                        val userGroupEntry = user.result.children.first()
                                        database.getReference("/user_groups/${auth.currentUser?.uid}/has")
                                            .child("live/${userGroupEntry.key}/op")
                                            .setValue(DatabaseOp.DELETE)
                                        database.getReference("/user_groups/${auth.currentUser?.uid}/has")
                                            .child("live/${userGroupEntry.key}/data")
                                            .setValue(groupUID)
                                        userGroupEntry.ref.removeValue().addOnCompleteListener {
                                            callback()
                                        }
                                    }
                                }
                        }
                }
            }
    }

    fun getUser(uid: String, success: (user: User) -> Unit = {}, error: () -> Unit = {}) {
        database.getReference("/user/${uid}").get().addOnCompleteListener {
            if (it.result.exists()) {
                success(it.result.getValue(User::class.java)!!)
            } else {
                error()
            }
        }
    }

    fun addContact() {

    }

    fun getUsersByName(
        name: String,
        callback: () -> Unit,
        iterator: (key: String, user: User) -> Unit
    ) {
        val upper = name.substring(0, name.length - 1) + name[name.length - 1].inc()
        database.getReference("/user")
            .orderByChild("static/name").startAt(name).endBefore(upper).get()
            .addOnCompleteListener {
                it.result.children.forEach { user ->
                    iterator(user.key!!, user.child("static").getValue(User::class.java)!!)
                }
                callback()
            }
    }

    fun setUserName(name: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}")
        database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
            .setValue(UserEdit.NAME.ordinal)
        ref.child("name").setValue(name).addOnCompleteListener {
            callback()
        }
    }

    fun setUserDescription(description: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}")
        database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
            .setValue(UserEdit.DESCRIPTION.ordinal)
        ref.child("description").setValue(description).addOnCompleteListener {
            callback()
        }
    }

    fun setUserProfile(profile: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}")
        database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
            .setValue(UserEdit.PROFILE.ordinal)
        ref.child("profile").setValue(profile).addOnCompleteListener {
            callback()
        }

    }

    fun setUserStatus(status: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}")
        database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
            .setValue(UserEdit.STATUS.ordinal)
        ref.child("status").setValue(status).addOnCompleteListener {
            callback()
        }
    }

    fun setUserData(user: User, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}")
        ref.child("name").setValue(user.name)
        ref.child("description").setValue(user.description)
        ref.child("profile").setValue(user.profile)
        ref.child("status").setValue(user.status)
        database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
            .setValue(UserEdit.CREATED.ordinal)
        callback()
    }

    fun createNewUser(callback: (user: User) -> Unit = {}) {
        val user = User(
            auth.currentUser?.displayName,
            auth.currentUser?.email,
            "Hello Flantern!",
            Status.ACTIVE.ordinal,
            "8bcbd691-ba4f-4f32-bce2-dff8d4412b66"
        )
        database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
            .setValue(UserEdit.CREATED.ordinal)
        database.getReference("user/${auth.currentUser?.uid}/static").setValue(user)
            .addOnCompleteListener {
                callback(user)
            }
    }

    fun getGroup(groupUID: String, success: (group: Group) -> Unit = {}, error: () -> Unit = {}) {
        database.getReference("/groups/$groupUID/static").get().addOnCompleteListener {
            if (it.result.exists()) {
                val group = it.result.getValue(Group::class.java)!!
                success(group)
            } else {
                error()
            }
        }
    }

    fun createGroup(group: Group, users: ArrayList<String>, callback: (key: String) -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("static").setValue(group).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.CREATED.ordinal)
            users.forEach {
                val groupUserRef =
                    database.getReference("/group_users/${ref.key}/has")
                val staticRef = groupUserRef.child("static").push()
                staticRef.setValue(it)
                groupUserRef.child("live/${staticRef.key}/op").setValue(DatabaseOp.ADD.ordinal)
                val userGroupsRef = database.getReference("/user_groups/${it}/has")
                val groupStaticRef = userGroupsRef.child("static").push()
                groupStaticRef.setValue(ref.key)
                userGroupsRef.child("live/${groupStaticRef.key}/op")
                    .setValue(DatabaseOp.ADD.ordinal)
            }
            val msgRef =
                database.getReference("/group_messages/${ref.key}/static")
                    .push()
            msgRef.setValue(
                Message(
                    "Flantern",
                    "You've created a group!",
                    System.currentTimeMillis()
                )
            )
            database.getReference("/group_messages/${ref.key}/live/${msgRef.key}/op")
                .setValue(DatabaseOp.ADD.ordinal)
            callback(ref.key!!)
        }
    }

    fun setGroupName(name: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("name").setValue(name).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.NAME.ordinal)
                .addOnCompleteListener {
                    callback()
                }
        }
    }

    fun setGroupDescription(description: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("description").setValue(description).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.DESCRIPTION.ordinal)
                .addOnCompleteListener {
                    callback()
                }
        }
    }

    fun setGroupProfile(profile: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("profile").setValue(profile).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.PROFILE.ordinal)
                .addOnCompleteListener {
                    callback()
                }
        }
    }

    fun setGroupData(group: Group, callback: () -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("name").setValue(group.name).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.NAME.ordinal)
                .addOnCompleteListener {
                    ref.child("description").setValue(group.description).addOnCompleteListener {
                        ref.child("live").push().child("op").setValue(GroupEdit.DESCRIPTION.ordinal)
                            .addOnCompleteListener {
                                ref.child("profile").setValue(group.profile).addOnCompleteListener {
                                    ref.child("live").push().child("op")
                                        .setValue(GroupEdit.PROFILE.ordinal)
                                        .addOnCompleteListener {
                                            callback()
                                        }
                                }
                            }
                    }
                }
        }
    }

    fun deleteGroup(groupUID: String, callback: (group: Group) -> Unit = {}) {
        //todo: add deletes for all other group references (don't forget storage!)
        val ref = database.getReference("/groups/$groupUID")
        ref.child("static").get().addOnCompleteListener {
            if (it.result.exists()) {
                val group = it.result.getValue(Group::class.java)!!
                ref.child("static").removeValue().addOnCompleteListener {
                    ref.child("live").push().child("op").setValue(GroupEdit.DELETED)
                        .addOnCompleteListener {
                            ref.child("live").push().child("data").setValue(group.name)
                                .addOnCompleteListener {
                                    callback(group)
                                }
                        }
                }
            }
        }
    }

    fun joinGroupWithCode(
        code: String,
        success: (groupUID: String) -> Unit = {},
        error: () -> Unit = {}
    ) {
        database.getReference("/group_invites")
            .child(code).get().addOnCompleteListener { mCode ->
                if (mCode.result.exists()) {
                    val groupUID = mCode.result.getValue(String::class.java)!!
                    val groupUsersRef = database.getReference("/group_users/$groupUID/has")
                    val groupUsersKey = groupUsersRef.child("static").push().key
                    groupUsersRef.child("static/$groupUsersKey").setValue(auth.currentUser?.uid)
                    groupUsersRef.child("live/$groupUsersKey/op").setValue(DatabaseOp.ADD)
                    val userGroupsRef =
                        database.getReference("/user_groups/${auth.currentUser?.uid}/has")
                    val userGroupsKey = groupUsersRef.child("static").push().key
                    userGroupsRef.child("static/$userGroupsKey").setValue(groupUID)
                    userGroupsRef.child("live/$userGroupsKey/op").setValue(DatabaseOp.ADD)
                    success(groupUID)
                } else {
                    error()
                }
            }
    }

    fun addContact(contactUID: String, callback: (userUID: String) -> Unit = {}) {
        //database.getReference("user_contacts/${auth.uid}/has/static/$contactUID").setValue()
    }

    fun createGroupInvite(groupUID: String, callback: (code: String) -> Unit = {}) {
        val code = (context as MainActivity).adjectives.random() +
                context.animals.random()
        context.rtDatabase.getReference("/group_invites/$code")
            .setValue(groupUID).addOnCompleteListener {
                callback(code)
            }
    }

    fun getGroupMediaBitmap(
        profileID: String,
        groupID: String,
        success: (Bitmap) -> Unit = {},
        error: () -> Unit = {}
    ) {
        storage.getReference("$groupID/${profileID}.jpg")
            .getBytes(ONE_MEGABYTE).addOnCompleteListener { image ->
                if (image.result.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(
                        image.result,
                        0,
                        image.result.size
                    )
                    success(bitmap)

                } else {
                    error()
                }
            }
    }

    fun setGroupMediaBitmap(
        profileID: String?,
        groupID: String,
        imageURI: Uri,
        callback: (profileID: String) -> Unit = {}
    ) {
        var imageID: String = profileID ?: UUID.randomUUID().toString()
        val outputStream = ByteArrayOutputStream()
        BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(
                imageURI
            )!!
        ).compress(Bitmap.CompressFormat.JPEG, 10, outputStream)
        storage.getReference("$groupID/$imageID.jpg")
            .putBytes(
                outputStream.toByteArray()
            ).addOnCompleteListener {
                callback(imageID)
            }
    }

    fun getGroupMediaDocumentUri(
        mediaID: Embed,
        groupID: String,
        callback: (mediaURI: Uri) -> Unit = {}
    ) {
        storage.getReference("$groupID/${mediaID.ref}.${mediaID.ext}").downloadUrl.addOnCompleteListener {
            callback(it.result)
        }
    }

    fun setGroupMediaDocument(
        mediaID: String?,
        groupID: String,
        documentUri: Uri,
        callback: (profileID: String) -> Unit = {}
    ) {
        val extension = getMimeType(context, documentUri)
        var documentID: String = mediaID ?: UUID.randomUUID().toString()
        storage.getReference("$groupID/$documentID.$extension").putStream(
            context.contentResolver.openInputStream(documentUri)!!
        )
    }

    fun getUserProfileBitmap(
        profileID: String,
        success: (Bitmap) -> Unit = {},
        error: () -> Unit = {}
    ) {
        storage.getReference("users/${profileID}.jpg")
            .getBytes(ONE_MEGABYTE).addOnCompleteListener { image ->
                if (image.result.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(
                        image.result,
                        0,
                        image.result.size
                    )
                    success(bitmap)
                } else {
                    error()
                }
            }
    }

    fun setUserProfileBitmap(
        profileID: String?,
        imageURI: Uri,
        callback: (profileID: String) -> Unit = {}
    ) {
        var imageID: String = ""
        if (profileID == null) {
            imageID = UUID.randomUUID().toString()
        } else {
            imageID = profileID
        }
        val outputStream = ByteArrayOutputStream()
        BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(
                imageURI
            )!!
        ).compress(Bitmap.CompressFormat.JPEG, 10, outputStream)
        storage.getReference("users/${imageID}.jpg")
            .putBytes(
                outputStream.toByteArray()
            ).addOnCompleteListener {
                callback(imageID)
            }
    }

    fun addUsersToGroup(groupUID: String, users: ArrayList<String>, callback: () -> Unit = {}) {
        val groupUsersRef = database.getReference("/group_users/$groupUID/has")
        for (i in users) {
            val groupUsersKey = groupUsersRef.child("static").push().key
            val userRef = database.getReference("/user_groups/$i/has")
            val userGroupsKey = groupUsersRef.child("static").push().key
            groupUsersRef.child("static/$groupUsersKey").setValue(i)
            groupUsersRef.child("live/$groupUsersKey/op").setValue(DatabaseOp.ADD)
            userRef.child("static/$userGroupsKey").setValue(groupUID)
            userRef.child("live/$userGroupsKey/op").setValue(DatabaseOp.ADD)
        }
        callback()
    }
}