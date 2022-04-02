package com.picobyte.flantern.requests

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.authentication.AuthGoogle
import com.picobyte.flantern.types.*
import com.picobyte.flantern.utils.ONE_MEGABYTE
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.util.*
class FlanternRequests(
    val activity: Activity,
    val database: FirebaseDatabase,
    val storage: FirebaseStorage,
    val auth: AuthGoogle
) {
    fun addMessage(item: Message) {
        val ref = database.getReference("group_messages")
        val key = ref.child("static").push().key!!
        ref.child("static").child(key).setValue(item)
        ref.child("live").child(key).setValue(MessageEdit.ADD.ordinal)
    }

    fun removeMessage(key: String) {
        val ref = database.getReference("group_messages")
        ref.child("static").child(key).removeValue()
        ref.child("live").child(key).setValue(MessageEdit.DELETE.ordinal)
    }

    fun modifyMessage(key: String, item: Message) {
        val ref = database.getReference("group_messages")
        ref.child("static").child(key).setValue(item)
        ref.child("live").child(key).setValue(MessageEdit.MODIFY.ordinal)
    }
    fun getLoggedInUser(callback: (user: User) -> Unit) {
        Jsoup.connect("https://jsoup.org/cookbook/extracting-data/attributes-text-html")
        database.getReference("/user/${auth.getUID()}").get().addOnCompleteListener {
            if (it.result.exists()) {
                callback(it.result.getValue(User::class.java)!!)
            }
        }
    }

    fun leaveGroup(groupUID: String, callback: () -> Unit) {
        val groupUsersRef = database.getReference("/group_users/$groupUID/has")
        groupUsersRef.child("static").equalTo(auth.getUID()).get()
            .addOnCompleteListener { user ->
                if (user.result.hasChildren()) {
                    val groupUserEntry = user.result.children.first()
                    groupUsersRef.child("static/${groupUserEntry.key}").removeValue()
                    groupUsersRef.child("live/${groupUserEntry.key}").setValue(DatabaseOp.DELETE)
                        .addOnCompleteListener {
                            database.getReference("/user_groups/${auth.getUID()}/has/static")
                                .equalTo(groupUID).get().addOnCompleteListener { user ->
                                    if (user.result.hasChildren()) {
                                        val userGroupEntry = user.result.children.first()
                                        groupUsersRef.child("live/${userGroupEntry.key}")
                                            .setValue(DatabaseOp.DELETE)
                                        userGroupEntry.ref.removeValue().addOnCompleteListener {
                                            callback()
                                        }
                                    }
                                }
                        }
                }
            }
    }

    fun getUser(uid: String, callback: (user: User) -> Unit) {
        database.getReference("/user/${uid}").get().addOnCompleteListener {
            if (it.result.exists()) {
                callback(it.result.getValue(User::class.java)!!)
            }
        }
    }

    fun setUserName(name: String, callback: () -> Unit) {
        val ref = database.getReference("/user/${auth.getUID()}")
        database.getReference("user/${auth.getUID()}/live").push().setValue(UserEdit.NAME.ordinal)
        ref.child("name").setValue(name).addOnCompleteListener {
            callback()
        }
    }

    fun setUserDescription(description: String, callback: () -> Unit) {
        val ref = database.getReference("/user/${auth.getUID()}")
        database.getReference("user/${auth.getUID()}/live").push()
            .setValue(UserEdit.DESCRIPTION.ordinal)
        ref.child("description").setValue(description).addOnCompleteListener {
            callback()
        }
    }

    fun setUserProfile(profile: String, callback: () -> Unit) {
        val ref = database.getReference("/user/${auth.getUID()}")
        database.getReference("user/${auth.getUID()}/live").push()
            .setValue(UserEdit.PROFILE.ordinal)
        ref.child("profile").setValue(profile).addOnCompleteListener {
            callback()
        }

    }

    fun setUserStatus(status: String, callback: () -> Unit) {
        val ref = database.getReference("/user/${auth.getUID()}")
        database.getReference("user/${auth.getUID()}/live").push().setValue(UserEdit.STATUS.ordinal)
        ref.child("status").setValue(status).addOnCompleteListener {
            callback()
        }
    }

    fun setUserData(user: User, callback: () -> Unit) {
        val ref = database.getReference("/user/${auth.getUID()}")
        ref.child("name").setValue(user.name)
        ref.child("description").setValue(user.description)
        ref.child("profile").setValue(user.profile)
        ref.child("status").setValue(user.status)
        database.getReference("user/${auth.getUID()}/live").push()
            .setValue(UserEdit.CREATED.ordinal)
        callback()
    }

    fun createNewUser(callback: (user: User) -> Unit) {
        val user = User(
            auth.getDisplayName(),
            auth.getEmail(),
            "Hello Flantern!",
            Status.ACTIVE.ordinal,
            "8bcbd691-ba4f-4f32-bce2-dff8d4412b66"
        )
        database.getReference("user/${auth.getUID()}/live").push()
            .setValue(UserEdit.CREATED.ordinal)
        database.getReference("user/${auth.getUID()}/static").setValue(user).addOnCompleteListener {
            callback(user)
        }
    }

    fun getGroup(groupUID: String, callback: (group: Group) -> Unit) {
        database.getReference("/groups/$groupUID/static").get().addOnCompleteListener {
            if (it.result.exists()) {
                val group = it.result.getValue(Group::class.java)!!
                callback(group)
            }
        }
    }

    fun createGroup(group: Group, users: Array<String>, callback: () -> Unit) {
        val ref = database.getReference("/groups").push()
        ref.setValue(group).addOnCompleteListener {
            users.forEach {
                val groupUserRef =
                    database.getReference("/group_users/${ref.key}/has")
                val staticRef = groupUserRef.child("static").push()
                staticRef.setValue(it)
                groupUserRef.child("live/${staticRef.key}").setValue(DatabaseOp.ADD.ordinal)
                val userGroupsRef = database.getReference("/user_groups/${it}/has")
                val groupStaticRef = userGroupsRef.child("static").push()
                groupStaticRef.setValue(ref.key)
                userGroupsRef.child("live/${groupStaticRef.key}")
                    .setValue(DatabaseOp.ADD.ordinal)
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
                database.getReference("/group_messages/${ref.key}/live/${msgRef.key}")
                    .setValue(DatabaseOp.ADD)
                callback()
            }
        }
    }

    fun setGroupName(name: String, callback: () -> Unit) {
        val ref = database.getReference("/groups").push()
        ref.child("name").setValue(name).addOnCompleteListener {
            ref.child("live").push().setValue(GroupEdit.NAME.ordinal).addOnCompleteListener {
                callback()
            }
        }
    }

    fun setGroupDescription(description: String, callback: () -> Unit) {
        val ref = database.getReference("/groups").push()
        ref.child("description").setValue(description).addOnCompleteListener {
            ref.child("live").push().setValue(GroupEdit.DESCRIPTION.ordinal).addOnCompleteListener {
                callback()
            }
        }
    }

    fun setGroupProfile(profile: String, callback: () -> Unit) {
        val ref = database.getReference("/groups").push()
        ref.child("profile").setValue(profile).addOnCompleteListener {
            ref.child("live").push().setValue(GroupEdit.PROFILE.ordinal).addOnCompleteListener {
                callback()
            }
        }
    }

    fun setGroupData(group: Group, callback: () -> Unit) {
        val ref = database.getReference("/groups").push()
        ref.child("name").setValue(group.name).addOnCompleteListener {
            ref.child("live").push().setValue(GroupEdit.NAME.ordinal).addOnCompleteListener {
                ref.child("description").setValue(group.description).addOnCompleteListener {
                    ref.child("live").push().setValue(GroupEdit.DESCRIPTION.ordinal)
                        .addOnCompleteListener {
                            ref.child("profile").setValue(group.profile).addOnCompleteListener {
                                ref.child("live").push().setValue(GroupEdit.PROFILE.ordinal)
                                    .addOnCompleteListener {
                                        callback()
                                    }
                            }
                        }
                }
            }
        }
    }

    fun deleteGroup(groupUID: String, callback: (group: Group)-> Unit) {
        //todo: add deletes for all other group references (don't forget storage!)
        val ref = database.getReference("/groups/$groupUID")
        ref.child("static").get().addOnCompleteListener {
            if (it.result.exists()) {
                val group = it.result.getValue(Group::class.java)!!
                ref.child("static").removeValue().addOnCompleteListener {
                    ref.child("live").push().setValue(GroupEdit.DELETED).addOnCompleteListener {
                        callback(group)
                    }
                }
            }
        }
    }

    fun joinGroupWithCode(code: String, callback: (groupUID: String) -> Unit) {
        database.getReference("/group_invites")
            .child(code).get().addOnCompleteListener { mCode ->
                if (mCode.result.exists()) {
                    val groupUID = mCode.result.getValue(String::class.java)!!
                    val groupUsersRef = database.getReference("/group_users/$groupUID/has")
                    val groupUsersKey = groupUsersRef.child("static").push().key
                    groupUsersRef.child("static/$groupUsersKey").setValue(auth.getUID())
                    groupUsersRef.child("live/$groupUsersKey").setValue(DatabaseOp.ADD)
                    val userGroupsRef =
                        database.getReference("/user_groups/${auth.getUID()}/has")
                    val userGroupsKey = groupUsersRef.child("static").push().key
                    userGroupsRef.child("static/$userGroupsKey").setValue(groupUID)
                    userGroupsRef.child("live/$userGroupsKey").setValue(DatabaseOp.ADD)
                    callback(groupUID)
                } else {
                }
            }
    }

    fun createGroupInvite(groupUID: String, callback: (code: String) -> Unit) {
        val code = (activity as MainActivity).adjectives.random() +
                activity.animals.random()
        activity.rtDatabase.getReference("/group_invites/$code")
            .setValue(groupUID).addOnCompleteListener {
                callback(code)
            }
    }

    fun getGroupProfile(profileID: String, groupID: String, callback: (Bitmap) -> Unit) {
        storage.getReference("$groupID/${profileID}.jpg")
            .getBytes(ONE_MEGABYTE).addOnCompleteListener { image ->
                val bitmap = BitmapFactory.decodeByteArray(
                    image.result,
                    0,
                    image.result.size
                )
                callback(bitmap)
            }
    }

    fun setGroupProfile(
        profileID: String?,
        groupID: String,
        imageURI: Uri,
        callback: (profileID: String) -> Unit
    ) {
        var imageID: String = profileID ?: UUID.randomUUID().toString()
        val outputStream = ByteArrayOutputStream()
        BitmapFactory.decodeStream(
            activity.contentResolver.openInputStream(
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

    fun getUserProfile(profileID: String, callback: (Bitmap) -> Unit) {
        storage.getReference("users/${profileID}.jpg")
            .getBytes(ONE_MEGABYTE).addOnCompleteListener { image ->
                val bitmap = BitmapFactory.decodeByteArray(
                    image.result,
                    0,
                    image.result.size
                )
                callback(bitmap)
            }
    }

    fun setUserProfile(profileID: String?, imageURI: Uri, callback: (profileID: String) -> Unit) {
        var imageID: String = ""
        if (profileID == null) {
            imageID = UUID.randomUUID().toString()
        } else {
            imageID = profileID
        }
        val outputStream = ByteArrayOutputStream()
        BitmapFactory.decodeStream(
            activity.contentResolver.openInputStream(
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

    fun addUsersToGroup(groupUID: String, users: Array<String>, callback: () -> Unit) {
        val groupUsersRef = database.getReference("/group_users/$groupUID/has")
        for (i in users) {
            val groupUsersKey = groupUsersRef.child("static").push().key
            groupUsersRef.child("static/$groupUsersKey").setValue(i)
            groupUsersRef.child("live/$groupUsersKey").setValue(DatabaseOp.ADD)
            val userRef = database.getReference("/user_groups/$i/has")
            val userGroupsKey = groupUsersRef.child("static").push().key
            userRef.child("static/$userGroupsKey").setValue(groupUID)
            userRef.child("live/$userGroupsKey").setValue(DatabaseOp.ADD)
        }
    }
}