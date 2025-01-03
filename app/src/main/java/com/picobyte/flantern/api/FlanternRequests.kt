package com.picobyte.flantern.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserInfo
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
    fun addMessage(
        groupUID: String, item: Message,
        success: (msgKey: String) -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.hasGroup(groupUID, auth.uid!!) {
            if (it) {
                val ref = database.getReference("group_messages/$groupUID")
                val key = ref.child("static").push().key!!
                ref.child("static/$key").setValue(item).addOnCompleteListener { msg ->
                    if (msg.isSuccessful) {
                        ref.child("live/$key/op").setValue(DatabaseOp.ADD.ordinal)
                        success(key)
                    } else {
                        error("Error occurred while adding message, check your internet connection and try again")
                    }
                }
            } else {
                error("Unable to send message. User is not in group")
            }
        }
    }

    fun removeMessage(
        groupUID: String,
        key: String,
        success: (msg: Message) -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.hasGroup(groupUID, auth.uid!!) {
            if (it) {
                val ref = database.getReference("group_messages/$groupUID")
                ref.child("static/$key").get().addOnCompleteListener { item ->
                    if (item.result.exists()) {
                        val entryKey = ref.child("live").push().key
                        ref.child("static/$key").removeValue().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                ref.child("live/$entryKey/op").setValue(DatabaseOp.DELETE.ordinal)
                                ref.child("live/$entryKey/data")
                                    .setValue(key)
                                success(item.result.getValue(Message::class.java)!!)
                            } else {
                                error("Error occurred while deleting message, check your internet connection and try again")
                            }
                        }
                    }
                }
            } else {
                error("Unable to delete message. User is not in group")
            }
        }
    }

    fun modifyMessage(
        groupUID: String,
        key: String,
        item: Message,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.hasGroup(groupUID, auth.uid!!) {
            if (it) {
                val ref = database.getReference("group_messages/$groupUID")
                val entryKey = ref.child("live").push().key
                ref.child("static").child(key).setValue(item).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        ref.child("live/$entryKey/op").setValue(DatabaseOp.MODIFY.ordinal)
                        ref.child("live/$entryKey/data")
                            .setValue(key)
                        success()
                    } else {
                        error("Error occurred while modifying message, check your internet connection and try again")
                    }
                }

            } else {
                error("Unable to modify message. User is not in group")
            }
        }
    }

    fun modifyMessageContent(
        groupUID: String,
        key: String,
        description: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.hasGroup(groupUID, auth.uid!!) {
            if (it) {
                val ref = database.getReference("group_messages/$groupUID")
                val entryKey = ref.child("live").push().key
                ref.child("static/$key/content").setValue(description)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            ref.child("live/$entryKey/op").setValue(DatabaseOp.MODIFY.ordinal)
                            ref.child("live/$entryKey/data")
                                .setValue(key)
                            success()
                        } else {
                            error("Unable to modify message content")
                        }
                    }
            } else {
                error("Unable to modify message content. User is not in group")
            }
        }
    }

    fun getMessage(
        groupUID: String,
        key: String,
        success: (msg: Message) -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        database.getReference("group_messages/$groupUID/static/$key").get().addOnCompleteListener {
            if (it.result.exists()) {
                val msg = it.result.getValue(Message::class.java)!!
                success(msg)
            } else {
                error("Could not retrieve message, message does not exist")
            }
        }
    }

    fun getRecent(
        groupUID: String,
        success: (messageUID: String, msg: Message) -> Unit = { messageUID: String, message: Message -> },
        error: (s: String) -> Unit = {}
    ) {
        database.getReference("groups/$groupUID/static/recent").get().addOnCompleteListener {
            if (it.result.exists()) {
                success(it.result.key!!, it.result.getValue(Message::class.java)!!)
            } else {
                error("Could not retrieve message, recent message does not exist")
            }
        }
    }

    fun setRecent(
        groupUID: String,
        msg: Message,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        database.getReference("groups/$groupUID/static/recent").setValue(msg)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    success()
                } else {
                    error("Unable to set recent message")
                }
            }
    }

    fun getMemberCount(groupUID: String, callback: (memberCount: Int) -> Unit) {
        database.getReference("group_users/$groupUID/has/static/").get().addOnCompleteListener {
            if (it.isSuccessful) {
                callback(it.result.childrenCount.toInt())
            } else {
                callback(0)
            }
        }
    }

    fun getPinnedMessage(
        groupUID: String,
        success: (msgUID: String, msg: Message) -> Unit = { s: String, message: Message -> },
        error: (s: String) -> Unit = {}
    ) {
        database.getReference("/groups/$groupUID/static/pin").get()
            .addOnCompleteListener { task ->
                if (task.result.exists()) {
                    val messageKey = task.result.getValue(String::class.java)!!
                    database.getReference("/group_messages/$groupUID/static/$messageKey").get()
                        .addOnCompleteListener { entry ->
                            if (entry.result.exists()) {
                                val message = entry.result.getValue(Message::class.java)!!
                                success(messageKey, message)
                            } else {
                                error("Pinned Message was deleted since last pinned")
                            }
                        }
                } else {
                    error("Unable to get pinned message.")
                }
            }
    }

    fun pinMessage(
        groupUID: String,
        msgKey: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                database.getReference("/groups/$groupUID/static/pin").setValue(msgKey)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            database.getReference("/groups/$groupUID/live").push()
                                .setValue(GroupEdit.PINNED.ordinal)
                            success()
                        } else {
                            error("Failed to pin Message")
                        }
                    }
            } else {
                error("Failed to pin message. User does not have admin permissions")
            }
        }
    }

    fun getLoggedInUser(success: (user: User) -> Unit = {}, error: (s: String) -> Unit = {}) {
        //Jsoup.connect("https://jsoup.org/cookbook/extracting-data/attributes-text-html")
        database.getReference("/user/${auth.currentUser?.uid}/static").get().addOnCompleteListener {
            if (it.result.exists()) {
                success(it.result.getValue(User::class.java)!!)
            } else {
                error("User does not exist")
            }
        }
    }

    fun isAdmin(userUID: String, groupUID: String, callback: (isAdmin: Boolean) -> Unit = {}) {
        Log.e("Flantern", userUID)
        Log.e("Flantern", groupUID)

        database.getReference("/group_users/$groupUID/admin/static").get()
            .addOnCompleteListener {
                var temp = false
                for (i in it.result.children) {
                    if (i.getValue(String::class.java) == userUID) {
                        callback(true)
                        temp = true
                        break
                    }
                }
                if (!temp) {
                    callback(false)
                }
            }
    }

    fun makeAdmin(
        userUID: String,
        groupUID: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                val groupAdminRef = database.getReference("/group_users/$groupUID/admin")
                val userAdminRef = database.getReference("/user_groups/$userUID/admin")
                val groupAdminKey = groupAdminRef.child("static").push().key!!
                val userAdminKey = userAdminRef.child("static").push().key!!
                groupAdminRef.child("static/$groupAdminKey").setValue(userUID)
                    .addOnCompleteListener {
                        userAdminRef.child("static/$userAdminKey").setValue(groupUID)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    groupAdminRef.child("live/$groupAdminKey")
                                        .setValue(DatabaseOp.ADD.ordinal)
                                    userAdminRef.child("live/$groupAdminKey")
                                        .setValue(DatabaseOp.ADD.ordinal)
                                    success()
                                } else {
                                    error("Failed to promote to admin")
                                }
                            }
                    }
            } else {
                error("Failed to promote to admin. User does not have admin perms")
            }
        }
    }

    fun removeAdmin(
        userUID: String,
        groupUID: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                val groupAdminRef = database.getReference("/group_users/$groupUID/admin")
                val userAdminRef = database.getReference("/user_groups/$userUID/admin")
                groupAdminRef.child("static").equalTo(userUID).get().addOnCompleteListener { item ->
                    if (item.result.hasChildren()) {
                        item.result.children.first().ref.removeValue()
                        userAdminRef.child("static").equalTo(groupUID).get()
                            .addOnCompleteListener { user ->
                                if (user.result.hasChildren()) {
                                    user.result.children.first().ref.removeValue()
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                groupAdminRef.child("live").push()
                                                    .setValue(DatabaseOp.ADD.ordinal)
                                                userAdminRef.child("live").push()
                                                    .setValue(DatabaseOp.ADD.ordinal)
                                                success()
                                            } else {
                                                error("Failed to remove admin perms")
                                            }
                                        }
                                }
                            }
                    }
                }
            } else {
                error("Failed to remove admin perms. User does not have admin perms")
            }
        }
    }

    fun isBlacklisted(
        userUID: String,
        groupUID: String,
        callback: (isBlackListed: Boolean) -> Unit = {}
    ) {
        database.getReference("/group_users/$groupUID/blacklist/static").equalTo(userUID).get()
            .addOnCompleteListener {
                callback(it.result.hasChildren())
            }
    }

    fun blacklistUser(
        userUID: String,
        groupUID: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                val groupUsersKey =
                    database.getReference("/group_users/$groupUID/blacklist/static").push()
                val userGroupKey =
                    database.getReference("/user_groups/$userUID/blacklist/static").push()
                database.getReference("/group_users/$groupUID/blacklist/static/$groupUsersKey")
                    .setValue(userUID).addOnCompleteListener {
                        database.getReference("/user_groups/$userUID/blacklist/static/$userGroupKey")
                            .setValue(groupUID).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    database.getReference("/group_users/$groupUID/blacklist/live/$groupUsersKey")
                                        .setValue(DatabaseOp.ADD.ordinal)
                                    database.getReference("/user_groups/$userUID/blacklist/live/$userGroupKey")
                                        .setValue(DatabaseOp.ADD.ordinal)
                                    success()
                                } else {
                                    error("Failed to blacklist user")
                                }
                            }
                    }
            } else {
                error("Failed to blacklist user. User does not have admin perms")
            }
        }
    }

    fun whitelistUser(
        userUID: String,
        groupUID: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                database.getReference("/group_users/$groupUID/blacklist/static").equalTo(userUID)
                    .get()
                    .addOnCompleteListener { item ->
                        if (item.result.hasChildren()) {
                            item.result.children.first().ref.removeValue()
                            database.getReference("/user_groups/$userUID/blacklist/static")
                                .equalTo(groupUID).get().addOnCompleteListener { item ->
                                    if (item.result.hasChildren()) {
                                        item.result.children.first().ref.removeValue()
                                            .addOnCompleteListener {
                                                database.getReference("/user_groups/$userUID/blacklist/live")
                                                    .push()
                                                    .setValue(DatabaseOp.DELETE.ordinal)
                                                database.getReference("/group_users/$groupUID/blacklist/live")
                                                    .push()
                                                    .setValue(DatabaseOp.DELETE.ordinal)
                                                success()
                                            }
                                    } else {
                                        error("Failed to whitelist user")
                                    }
                                }
                        }
                    }
            } else {
                error("Failed to whitelist user. User does not have admin perms")
            }
        }
    }

    fun isUserInGroup(
        userUID: String,
        groupUID: String,
        callback: (isUserInGroup: Boolean) -> Unit = {}
    ) {
        database.getReference("/group_users/$groupUID/has/static").get().addOnCompleteListener {
            var temp = false
            for (i in it.result.children) {
                if (i.getValue(String::class.java) == userUID) {
                    callback(true)
                    temp = true
                    break
                }
            }
            if (!temp) {
                callback(false)
            }
        }
    }
    data class LiveEntryFlantern(val op: Int? = null, val data: String?  = null)
    fun kickUser(userUID: String, groupUID: String, callback: () -> Unit = {}) {
        val groupUsersRef = database.getReference("/group_users/$groupUID/has")
        groupUsersRef.child("static").get().addOnCompleteListener {
            for (i in it.result.children) {
                Log.e("Flantern", "${i.getValue(String::class.java)} ${i.key} $userUID")
                if (i.getValue(String::class.java) == userUID) {
                    groupUsersRef.child("static/${i.key}").removeValue().addOnCompleteListener {
                        database.getReference("user_groups/$userUID/has/static/${i.key}")
                            .removeValue().addOnCompleteListener {
                                val newKey = database.getReference("/user_groups/${userUID}/has").push().key

                                database.getReference("/user_groups/${userUID}/has")
                                    .child("live/$newKey")
                                    .setValue(LiveEntryFlantern(DatabaseOp.DELETE.ordinal, i.key))
                                groupUsersRef.child("live/$newKey")
                                    .setValue(LiveEntryFlantern(DatabaseOp.DELETE.ordinal, i.key))
                                callback()
                            }
                    }
                }
            }
        }
        /*groupUsersRef.child("static").equalTo(userUID).get()
            .addOnCompleteListener { user ->
                if (user.result.hasChildren()) {
                    val groupUserEntry = user.result.children.first()
                    groupUsersRef.child("static/${groupUserEntry.key}").removeValue()
                        .addOnCompleteListener {
                            database.getReference("/user_groups/${userUID}/has/static")
                                .equalTo(groupUID).get().addOnCompleteListener { user ->
                                    if (user.result.hasChildren()) {
                                        val userGroupEntry = user.result.children.first()
                                        userGroupEntry.ref.removeValue()
                                            .addOnCompleteListener {
                                                database.getReference("/user_groups/${userUID}/has")
                                                    .child("live/${userGroupEntry.key}/op")
                                                    .setValue(DatabaseOp.DELETE.ordinal)
                                                groupUsersRef.child("live/${groupUserEntry.key}/op")
                                                    .setValue(DatabaseOp.DELETE.ordinal)
                                                groupUsersRef.child("live/${groupUserEntry.key}/data")
                                                    .setValue(userUID)
                                                database.getReference("/user_groups/${userUID}/has")
                                                    .child("live/${userGroupEntry.key}/data")
                                                    .setValue(groupUID)
                                                callback()
                                            }
                                    }
                                }

                        }
                }
            }*/
    }

    fun hasGroup(groupUID: String, userUID: String, callback: (hasGroup: Boolean) -> Unit = {}) {
        database.getReference("/user_groups/$userUID/has/static").get()
            .addOnCompleteListener {
                var test = false
                for (i in it.result.children) {
                    if (i.getValue(String::class.java) == groupUID) {
                        callback(true)
                        test = true
                        break
                    }
                }
                if (!test) {
                    callback(false)
                }
            }
    }

    fun leaveGroup(groupUID: String, callback: () -> Unit = {}) {
        val groupUsersRef = database.getReference("/group_users/$groupUID/has")
        groupUsersRef.child("static").orderByValue().equalTo(auth.uid).get()
            .addOnCompleteListener {
                Log.e("Flantern", it.result.ref.toString())
                for (i in it.result.children) {
                    Log.e("Flantern", i.getValue(String::class.java)!!)
                }
            }

        /*groupUsersRef.child("static").equalTo(auth.currentUser?.uid).get()
            .addOnCompleteListener { user ->
                if (user.result.hasChildren()) {
                    val groupUserEntry = user.result.children.first()
                    groupUsersRef.child("static/${groupUserEntry.key}").removeValue()
                        .addOnCompleteListener {
                            database.getReference("/user_groups/${auth.currentUser?.uid}/has/static")
                                .equalTo(groupUID).get().addOnCompleteListener { user ->
                                    if (user.result.hasChildren()) {
                                        val userGroupEntry = user.result.children.first()
                                        userGroupEntry.ref.removeValue()
                                            .addOnCompleteListener {
                                                groupUsersRef.child("live/${groupUserEntry.key}/op")
                                                    .setValue(DatabaseOp.DELETE.ordinal)
                                                groupUsersRef.child("live/${groupUserEntry.key}/data")
                                                    .setValue(auth.currentUser?.uid)
                                                database.getReference("/user_groups/${auth.currentUser?.uid}/has")
                                                    .child("live/${userGroupEntry.key}/op")
                                                    .setValue(DatabaseOp.DELETE.ordinal)
                                                database.getReference("/user_groups/${auth.currentUser?.uid}/has")
                                                    .child("live/${userGroupEntry.key}/data")
                                                    .setValue(groupUID)
                                                callback()
                                            }
                                    }
                                }

                        }
                }
            }*/
    }

    fun getUser(uid: String, success: (user: User) -> Unit = {}, error: (s: String) -> Unit = {}) {
        database.getReference("/user/${uid}/static").get().addOnCompleteListener {
            if (it.result.exists()) {
                success(it.result.getValue(User::class.java)!!)
            } else {
                error("User does not exist")
            }
        }
    }

    fun getUserName(
        uid: String,
        success: (name: String) -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        database.getReference("/user/${uid}/static/name").get().addOnCompleteListener {
            if (it.result.exists()) {
                success(it.result.getValue(String::class.java)!!)
            } else {
                error("User does not exist")
            }
        }
    }

    fun listenForUserName(uid: String, callback: (name: String) -> Unit) {
        database.getReference("/user/${uid}/live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val operation = snapshot.child("op").getValue(Int::class.java)!!
                    if (operation == UserEdit.NAME.ordinal)
                        database.getReference("/user/${uid}/static/name").get()
                            .addOnCompleteListener {
                                if (it.result.exists()) {
                                    callback(it.result.getValue(String::class.java)!!)
                                }
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

    fun hasContact(contactUID: String, userUID: String, callback: (hasContact: Boolean) -> Unit) {
        database.getReference("/user_contacts/$contactUID/has/static").get().addOnCompleteListener {
            var temp = false
            for (i in it.result.children) {
                if (i.getValue(String::class.java) == contactUID) {
                    callback(true)
                    temp = true
                    break
                }
            }
            if (!temp) {
                callback(false)
            }
        }
    }

    fun addContact(contactUID: String, success: () -> Unit = {}, error: (s: String) -> Unit = {}) {
        val ref = database.getReference("/user_contacts/$contactUID/has")
        val key = ref.child("static").push().key
        this.hasContact(contactUID, auth.uid!!) {
            if (it) {
                error("Contact already added")
            } else {
                ref.child("static/$key").setValue(contactUID).addOnCompleteListener {
                    if (contactUID != auth.uid) {
                        val otherRef =
                            database.getReference("/user_contacts/$contactUID/has")
                        otherRef.child("static/$key").setValue(auth.uid)
                            .addOnCompleteListener {
                                ref.child("live/${key}/op").setValue(DatabaseOp.ADD.ordinal)
                                otherRef.child("live/$key/op").setValue(DatabaseOp.ADD.ordinal)
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
        ref.child("name").setValue(name).addOnCompleteListener {
            database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
                .setValue(UserEdit.NAME.ordinal)
            callback()
        }
    }

    fun setUserDescription(description: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}")
        ref.child("description").setValue(description).addOnCompleteListener {
            database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
                .setValue(UserEdit.DESCRIPTION.ordinal)
            callback()
        }
    }

    fun setUserProfile(profile: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}")
        ref.child("profile").setValue(profile).addOnCompleteListener {
            database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
                .setValue(UserEdit.PROFILE.ordinal)
            callback()
        }

    }

    fun setUserStatus(status: String, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}")
        ref.child("status").setValue(status).addOnCompleteListener {
            database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
                .setValue(UserEdit.STATUS.ordinal)
            callback()
        }
    }

    fun setUserData(user: User, callback: () -> Unit = {}) {
        val ref = database.getReference("/user/${auth.currentUser?.uid}/static")
        ref.child("name").setValue(user.name).addOnCompleteListener {
            ref.child("description").setValue(user.description).addOnCompleteListener {
                ref.child("profile").setValue(user.profile).addOnCompleteListener {
                    ref.child("status").setValue(user.status).addOnCompleteListener {
                        database.getReference("user/${auth.currentUser?.uid}/live").push()
                            .child("op")
                            .setValue(UserEdit.NAME.ordinal)
                        database.getReference("user/${auth.currentUser?.uid}/live").push()
                            .child("op")
                            .setValue(UserEdit.DESCRIPTION.ordinal)
                        database.getReference("user/${auth.currentUser?.uid}/live").push()
                            .child("op")
                            .setValue(UserEdit.PROFILE.ordinal)
                        database.getReference("user/${auth.currentUser?.uid}/live").push()
                            .child("op")
                            .setValue(UserEdit.STATUS.ordinal)
                        callback()
                    }
                }
            }
        }
    }

    fun createNewUser(callback: (user: User) -> Unit = {}) {
        var user: User
        if (auth.currentUser?.displayName==null) {
            user = User(
                "Flantern User",
                auth.currentUser?.email,
                "Hello Flantern!",
                Status.ACTIVE.ordinal,
                "8bcbd691-ba4f-4f32-bce2-dff8d4412b66"
            )
        } else {
            user = User(
                auth.currentUser?.displayName,
                auth.currentUser?.email,
                "Hello Flantern!",
                Status.ACTIVE.ordinal,
                "8bcbd691-ba4f-4f32-bce2-dff8d4412b66"
            )
        }

        database.getReference("user/${auth.currentUser?.uid}/static").setValue(user)
            .addOnCompleteListener {
                database.getReference("user/${auth.currentUser?.uid}/live").push().child("op")
                    .setValue(UserEdit.CREATED.ordinal)
                callback(user)
            }
    }

    fun getGroup(
        groupUID: String,
        success: (group: Group) -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        database.getReference("/groups/$groupUID/static").get().addOnCompleteListener {
            if (it.result.exists()) {
                val group = it.result.getValue(Group::class.java)!!
                success(group)
            } else {
                error("Group does not exist")
            }
        }
    }

    fun createGroup(group: Group, users: ArrayList<String>, callback: (key: String) -> Unit = {}) {
        val ref = database.getReference("/groups").push()
        Log.e("Flantern", "hello edit group am triggering")
        ref.child("static").setValue(group).addOnCompleteListener {
            Log.e("Flantern user size", users.size.toString())
            users.forEach {
                val groupUserRef =
                    database.getReference("/group_users/${ref.key}/has")
                val staticRef = groupUserRef.child("static").push()
                staticRef.setValue(it).addOnCompleteListener { _ ->
                    groupUserRef.child("live/${staticRef.key}/op").setValue(DatabaseOp.ADD.ordinal)
                    val userGroupsRef = database.getReference("/user_groups/${it}/has")
                    val groupStaticRef = userGroupsRef.child("static/${staticRef.key}")
                    userGroupsRef.child("live/${staticRef.key}/op")
                        .setValue(DatabaseOp.ADD.ordinal)
                    groupStaticRef.setValue(ref.key)
                }
            }
            val groupAdminRef = database.getReference("/group_users/${ref.key}/admin")
            val userAdminRef = database.getReference("/user_groups/${auth.uid}/admin")
            val key = groupAdminRef.child("static").push().key!!
            groupAdminRef.child("static/$key").setValue(auth.uid)
                .addOnCompleteListener {
                    userAdminRef.child("static/$key").setValue(ref.key)
                        .addOnCompleteListener {
                            val msgRef =
                                database.getReference("/group_messages/${ref.key}/static")
                                    .push()
                            msgRef.setValue(
                                Message(
                                    "Flantern",
                                    "You've created a group!",
                                    System.currentTimeMillis()
                                )
                            ).addOnCompleteListener {
                                database.getReference("/group_messages/${ref.key}/live/${msgRef.key}/op")
                                    .setValue(DatabaseOp.ADD.ordinal)
                                callback(ref.key!!)
                            }
                        }
                }
            groupAdminRef.child("live/$key").setValue(DatabaseOp.ADD.ordinal)
            userAdminRef.child("live/$key").setValue(DatabaseOp.ADD.ordinal)
            ref.child("live").push().child("op").setValue(GroupEdit.CREATED.ordinal)
        }
    }

    fun setGroupName(
        groupUID: String,
        name: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                val ref = database.getReference("/groups/$groupUID")
                ref.child("static/name").setValue(name).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        ref.child("live").push().child("op").setValue(GroupEdit.NAME.ordinal)
                        success()
                    } else {
                        error("Failed to set group name")
                    }
                }
            } else {
                error("Failed to set group name, User is not a group admin")
            }
        }
    }

    fun setGroupDescription(
        groupUID: String,
        description: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                val ref = database.getReference("/groups/$groupUID")
                ref.child("static/description").setValue(description)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            ref.child("live").push().child("op")
                                .setValue(GroupEdit.DESCRIPTION.ordinal)
                            success()
                        } else {
                            error("Failed to set group description")
                        }
                    }
            } else {
                error("Failed to set group description, User is not a group admin")
            }
        }
    }

    fun setGroupProfile(
        groupUID: String,
        profile: String,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                val ref = database.getReference("/groups/$groupUID")
                ref.child("static/profile").setValue(profile).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        ref.child("live").push().child("op").setValue(GroupEdit.PROFILE.ordinal)
                        success()
                    } else {
                        error("Failed to set group profile")
                    }
                }
            } else {
                error("Failed to set group profile, User is not a group admin")
            }
        }
    }

    fun setGroupData(
        groupUID: String,
        group: Group,
        success: () -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                val ref = database.getReference("/groups/$groupUID")
                ref.child("static/name").setValue(group.name).addOnCompleteListener {
                    ref.child("static/description").setValue(group.description)
                        .addOnCompleteListener {
                            ref.child("static/profile").setValue(group.profile)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        ref.child("live").push().child("op")
                                            .setValue(GroupEdit.NAME.ordinal)
                                        ref.child("live").push().child("op")
                                            .setValue(GroupEdit.DESCRIPTION.ordinal)
                                        ref.child("live").push().child("op")
                                            .setValue(GroupEdit.PROFILE.ordinal)
                                        success()
                                    } else {
                                        error("Failed to set group details")
                                    }
                                }
                        }
                }

            } else {
                error("Failed to set group details, user does not have admin perms")
            }
        }
    }

    fun deleteGroup(groupUID: String, success: () -> Unit = {}, error: (s: String) -> Unit = {}) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                database.getReference("group_invites").equalTo(groupUID).get()
                    .addOnCompleteListener { task ->
                        task.result.children.forEach { entry ->
                            entry.ref.removeValue()
                        }
                        database.getReference("group_messages/$groupUID").removeValue()
                            .addOnCompleteListener { _ ->
                                database.getReference("group_users/$groupUID/has/static").get()
                                    .addOnCompleteListener { item ->
                                        item.result.children.forEach { entry ->
                                            database.getReference(
                                                "user_groups/${
                                                    entry.getValue(
                                                        String::class.java
                                                    )
                                                }/has/static"
                                            ).get().addOnCompleteListener { item1 ->
                                                item1.result.children.forEach { item2->
                                                    if (item2.getValue(String::class.java)==groupUID) {
                                                        item2.ref.removeValue()
                                                    }
                                                }
                                            }
                                        }
                                        database.getReference("group_users/$groupUID").removeValue()
                                            .addOnCompleteListener {
                                                database.getReference("groups/$groupUID/live/op")
                                                    .push().setValue(GroupEdit.DELETED.ordinal)
                                                success()
                                            }
                                    }
                            }
                    }
            } else {
                error("Failed to delete group, user does not have admin perms")
            }
        }
    }

    fun deleteUser() {

    }

    fun addUsersToGroup(groupUID: String, users: ArrayList<String>, callback: () -> Unit = {}) {
        val groupUsersRef = database.getReference("/group_users/$groupUID/has")
        Log.e("Flantern", users.toString())
        for (i in users) {
            val key = groupUsersRef.child("static").push().key
            val userRef = database.getReference("/user_groups/$i/has")
            userRef.child("static/$key").setValue(groupUID)
                .addOnCompleteListener {
                    groupUsersRef.child("static/$key").setValue(i).addOnCompleteListener {
                        groupUsersRef.child("live/$key/op").setValue(DatabaseOp.ADD.ordinal)
                        userRef.child("live/$key/op").setValue(DatabaseOp.ADD.ordinal)
                        callback()
                    }
                }

        }
    }

    fun joinGroupWithCode(
        code: String,
        success: (groupUID: String) -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        database.getReference("/group_invites")
            .child(code).get().addOnCompleteListener { mCode ->
                if (mCode.result.exists()) {
                    val groupUID = mCode.result.getValue(String::class.java)!!
                    this.isBlacklisted(auth.uid!!, groupUID) {
                        if (!it) {
                            val groupUsersRef = database.getReference("/group_users/$groupUID/has")
                            val key = groupUsersRef.child("static").push().key
                            val userGroupsRef =
                                database.getReference("/user_groups/${auth.uid}/has")
                            groupUsersRef.child("static/$key")
                                .setValue(auth.uid)
                                .addOnCompleteListener {
                                    userGroupsRef.child("static/$key").setValue(groupUID)
                                        .addOnCompleteListener {
                                            groupUsersRef.child("live/$key/op")
                                                .setValue(DatabaseOp.ADD.ordinal)
                                            userGroupsRef.child("live/$key/op")
                                                .setValue(DatabaseOp.ADD.ordinal)
                                            success(groupUID)
                                        }
                                }
                        } else {
                            error("Failed to join group, user is blacklisted")
                        }
                    }
                } else {
                    error("Failed to join group, invite code is invalid")
                }
            }
    }


    fun createGroupInvite(
        groupUID: String,
        success: (code: String) -> Unit = {},
        error: (s: String) -> Unit = {}
    ) {
        this.isAdmin(auth.uid!!, groupUID) {
            if (it) {
                val code = (context as MainActivity).adjectives.random() +
                        context.animals.random()
                context.rtDatabase.getReference("/group_invites/$code")
                    .setValue(groupUID).addOnCompleteListener {
                        success(code)
                    }
            } else {
                error("Failed to create group invite, user is not an admin")
            }
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
                if (image.isSuccessful) {
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
        imageResource: Int,
        callback: (profileID: String) -> Unit = {}
    ) {
        var imageID: String = profileID ?: UUID.randomUUID().toString()
        val outputStream = ByteArrayOutputStream()
        if (imageURI != Uri.EMPTY) {
            BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(
                    imageURI
                )!!
            ).compress(Bitmap.CompressFormat.JPEG, 1, outputStream)
            storage.getReference("$groupID/$imageID.jpg")
                .putBytes(
                    outputStream.toByteArray()
                ).addOnCompleteListener {
                    callback(imageID)
                }
        } else {
            BitmapFactory.decodeResource(context.resources, imageResource)
                .compress(Bitmap.CompressFormat.JPEG, 1, outputStream)
            Log.e("Flantern", "what")
            storage.getReference("$groupID/$imageID.jpg")
                .putBytes(
                    outputStream.toByteArray()
                ).addOnCompleteListener {
                    callback(imageID)
                }.addOnCanceledListener {
                    Log.e("Flantern", "what")
                }.addOnProgressListener {
                    Log.e("Flantern", it.bytesTransferred.toString())
                }
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
                if (image.isSuccessful) {
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
}