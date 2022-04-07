package com.picobyte.flantern.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.R
import com.picobyte.flantern.types.*
import com.picobyte.flantern.utils.ONE_MEGABYTE
import com.picobyte.flantern.utils.getMimeType
import com.picobyte.flantern.utils.navigateWithBundle
import java.io.ByteArrayOutputStream
import java.util.*

class FlanternRequests(
    val context: Context,
    val database: FirebaseDatabase,
    val storage: FirebaseStorage,
    val auth: FirebaseAuth
) {
    fun addMessage(groupUID: String, item: Message, callback: (msgKey: String) -> Unit = {}) {
        val ref = database.getReference("group_messages/$groupUID")
        val key = ref.child("static").push().key!!
        ref.child("live/$key/op").setValue(DatabaseOp.ADD.ordinal)
        ref.child("static/$key").setValue(item).addOnCompleteListener {
            callback(key)
        }
    }

    fun removeMessage(groupUID: String, key: String, callback: (msg: Message) -> Unit = {}) {
        val ref = database.getReference("group_messages/$groupUID")
        ref.child("static/$key/user").get().addOnCompleteListener {
            if (it.result.exists()) {
                val entryKey = ref.child("live").push().key
                ref.child("live/$entryKey/op").setValue(DatabaseOp.DELETE.ordinal)
                ref.child("live/$entryKey/data").setValue(it.result.getValue(String::class.java))
                ref.child("static/$key").removeValue().addOnCompleteListener { _ ->
                    callback(it.result.getValue(Message::class.java)!!)
                }
            }
        }
    }

    fun modifyMessage(groupUID: String, key: String, item: Message, callback: () -> Unit) {
        val ref = database.getReference("group_messages/$groupUID")
        val entryKey = ref.child("live").push().key
        ref.child("live/$entryKey/op").setValue(DatabaseOp.MODIFY.ordinal)
        ref.child("static").child(key).setValue(item).addOnCompleteListener {
            callback()
        }
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

    fun getRecent(
        groupUID: String,
        success: (messageUID: String, msg: Message) -> Unit = { messageUID: String, message: Message -> },
        error: () -> Unit = {}
    ) {
        database.getReference("groups/$groupUID/static/recent").get().addOnCompleteListener {
            if (it.result.exists()) {
                success(it.result.key!!, it.result.getValue(Message::class.java)!!)
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

    fun pinMessage(groupUID: String, msg: Message, callback: () -> Unit) {
        database.getReference("/groups/$groupUID/static/pin").setValue(msg).addOnCompleteListener {
            callback()
        }
    }

    fun getLoggedInUser(success: (user: User) -> Unit = {}, error: () -> Unit = {}) {
        //Jsoup.connect("https://jsoup.org/cookbook/extracting-data/attributes-text-html")
        database.getReference("/user/${auth.currentUser?.uid}/static").get().addOnCompleteListener {
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
                    groupUsersRef.child("live/${groupUserEntry.key}/op").setValue(DatabaseOp.DELETE)
                    groupUsersRef.child("static/${groupUserEntry.key}").removeValue()
                        .addOnCompleteListener {
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
                                                userGroupEntry.ref.removeValue()
                                                    .addOnCompleteListener {
                                                        callback()
                                                    }
                                            }
                                        }
                                }
                        }
                }
            }
    }

    fun getUser(uid: String, success: (user: User) -> Unit = {}, error: () -> Unit = {}) {
        database.getReference("/user/${uid}/static").get().addOnCompleteListener {
            if (it.result.exists()) {
                success(it.result.getValue(User::class.java)!!)
            } else {
                error()
            }
        }
    }

    fun addContact(contactUID: String, success: () -> Unit = {}, error: () -> Unit = {}) {
        val ref = database.getReference("/user_contacts/$contactUID/has")
        val key = ref.child("static").push().key
        ref.child("static").equalTo(contactUID).get().addOnCompleteListener {
            if (it.result.exists()) {
                error()
            } else {
                ref.child("static/$key").setValue(contactUID).addOnCompleteListener {
                    ref.child("live/${key}/op").setValue(DatabaseOp.ADD)
                    if (contactUID != auth.uid) {
                        val otherRef =
                            database.getReference("/user_contacts/$contactUID/has")
                        val otherKey = otherRef.child("static").push().key
                        otherRef.child("live/$otherKey/op").setValue(DatabaseOp.ADD)
                        otherRef.child("static/$otherKey").setValue(auth.uid)
                            .addOnCompleteListener {
                                success()
                            }
                    }
                }
            }
        }

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
        ref.child("name").setValue(user.name).addOnCompleteListener {
            ref.child("description").setValue(user.description).addOnCompleteListener {
                ref.child("profile").setValue(user.profile).addOnCompleteListener {
                    ref.child("status").setValue(user.status).addOnCompleteListener {
                        database.getReference("user/${auth.currentUser?.uid}/live").push()
                            .child("op")
                            .setValue(UserEdit.CREATED.ordinal)
                        callback()
                    }
                }
            }
        }
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
                staticRef.setValue(it).addOnCompleteListener { _ ->
                    groupUserRef.child("live/${staticRef.key}/op").setValue(DatabaseOp.ADD.ordinal)
                    val userGroupsRef = database.getReference("/user_groups/${it}/has")
                    val groupStaticRef = userGroupsRef.child("static").push()
                    userGroupsRef.child("live/${groupStaticRef.key}/op")
                        .setValue(DatabaseOp.ADD.ordinal)
                    groupStaticRef.setValue(ref.key).addOnCompleteListener {
                        callback(ref.key!!)
                    }
                }
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
        }
    }

    fun setGroupName(name: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("name").setValue(name).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.NAME.ordinal)
            callback()
        }
    }

    fun setGroupDescription(description: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("description").setValue(description).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.DESCRIPTION.ordinal)
            callback()
        }
    }

    fun setGroupProfile(profile: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("profile").setValue(profile).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.PROFILE.ordinal)
            callback()
        }
    }

    fun setGroupData(group: Group, callback: () -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        ref.child("name").setValue(group.name).addOnCompleteListener {
            ref.child("live").push().child("op").setValue(GroupEdit.NAME.ordinal)
            ref.child("description").setValue(group.description).addOnCompleteListener {
                ref.child("live").push().child("op").setValue(GroupEdit.DESCRIPTION.ordinal)
                ref.child("profile").setValue(group.profile).addOnCompleteListener {
                    ref.child("live").push().child("op")
                        .setValue(GroupEdit.PROFILE.ordinal)
                    callback()
                }
            }
        }
    }

    fun deleteGroup(groupUID: String, callback: () -> Unit = {}) {
        //todo: add deletes for all other group references (don't forget storage!)
        database.getReference("groups/$groupUID/live").push().setValue(GroupEdit.DELETED)
        database.getReference("group_invites").equalTo(groupUID).get()
            .addOnCompleteListener {
                it.result.children.forEach { entry ->
                    entry.ref.removeValue()
                }
                database.getReference("group_messages/$groupUID").removeValue()
                    .addOnCompleteListener { _ ->
                        database.getReference("group_users/$groupUID/has/static").get()
                            .addOnCompleteListener { item ->
                                item.result.children.forEach { entry ->
                                    database.getReference("user_groups/${entry.getValue(String::class.java)}/has/static/$groupUID")
                                        .removeValue()
                                }
                                database.getReference("group_users/$groupUID").removeValue()
                                    .addOnCompleteListener {
                                        callback()
                                    }
                            }
                    }
            }
    }

    fun deleteUser() {

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
                    groupUsersRef.child("live/$groupUsersKey/op").setValue(DatabaseOp.ADD)
                    val userGroupsRef =
                        database.getReference("/user_groups/${auth.currentUser?.uid}/has")
                    val userGroupsKey = groupUsersRef.child("static").push().key
                    userGroupsRef.child("live/$userGroupsKey/op").setValue(DatabaseOp.ADD)
                    groupUsersRef.child("static/$groupUsersKey").setValue(auth.currentUser?.uid)
                        .addOnCompleteListener {
                            userGroupsRef.child("static/$userGroupsKey").setValue(groupUID)
                                .addOnCompleteListener {
                                    success(groupUID)
                                }
                        }
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
        ).addOnCompleteListener {
            callback(documentID)
        }
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