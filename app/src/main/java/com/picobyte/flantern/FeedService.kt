package com.picobyte.flantern

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.picobyte.flantern.api.FlanternRequests
import com.picobyte.flantern.types.*

const val CHANNEL_ID = "com.picobyte.flantern"
const val ADD_GROUP_ID = "com.picobyte.flantern.group.add"
const val ADD_USER_ID = "com.picobyte.flantern.user.add"
const val GROUP_ID = "com.picobyte.flantern.group"
const val USER_ID = "com.picobyte.flantern.user"
const val GROUP_MESSAGE_ID = "com.picobyte.flantern.group.messages"

class FeedService : Service() {
    lateinit var auth: FirebaseAuth
    lateinit var database: FirebaseDatabase
    lateinit var storage: FirebaseStorage
    lateinit var requests: FlanternRequests
    val groupListeners = HashMap<String, ChildEventListener>()
    val userListeners = HashMap<String, ChildEventListener>()
    val groupMessageListeners = HashMap<String, ChildEventListener>()

    val addGroupNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("Group was added")
        .setSmallIcon(R.drawable.group)
        .setGroup(ADD_GROUP_ID)
    val addUserNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("User was added")
        .setSmallIcon(R.drawable.profile)
        .setGroup(ADD_USER_ID)
    val groupNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("Group details were changed")
        .setSmallIcon(R.drawable.group)
        .setGroup(GROUP_ID)
    val userNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("User details were changed")
        .setSmallIcon(R.drawable.message)
        .setGroup(USER_ID)
    val groupMessageNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("Message was sent in group")
        .setSmallIcon(R.drawable.document)
        .setGroup(GROUP_MESSAGE_ID)
    val groupNotifs = ArrayList<Notification>()
    val userNotifs = ArrayList<Notification>()
    val groupMessageNotifs = ArrayList<Notification>()
    val groupNotifIds = ArrayList<Int>()
    val userNotifIds = ArrayList<Int>()
    val groupMessageNotifIds = ArrayList<Int>()

    val unread = HashMap<String, Int>()
    override fun onBind(p0: Intent?): IBinder? {
        auth = Firebase.auth
        database = Firebase.database(getString(R.string.realtime_db_id))
        storage = Firebase.storage
        requests = FlanternRequests(this, database, storage, auth)
        groupNotifIds.add(7)
        groupNotifIds.add(19)
        groupNotifIds.add(42)
        userNotifIds.add(7)
        userNotifIds.add(19)
        userNotifIds.add(42)
        groupMessageNotifIds.add(7)
        groupMessageNotifIds.add(19)
        groupMessageNotifIds.add(42)
        return null
    }

    fun resetUnreadMessages(groupUID: String) {
        unread[groupUID] = 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userUID = intent!!.getStringExtra("user_uid")!!
        val userContactsRef = database.getReference("user_contacts/$userUID/has")
        val userGroupsRef = database.getReference("user_groups/$userUID/has")


        userGroupsRef.child("static").get().addOnCompleteListener {
            it.result.children.forEach { child ->
                val groupUID = child.getValue(String::class.java)!!
                unread[groupUID] = 0
                database.getReference("group_messages/$groupUID/live")
                    .addChildEventListener(object : ChildEventListener {
                        val uid = groupUID
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            if (snapshot.getValue(Int::class.java) == DatabaseOp.ADD.ordinal) {
                                unread[uid] = unread[uid]?.plus(1)!!
                            }
                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            return
                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {
                            return
                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            return
                        }

                        override fun onCancelled(error: DatabaseError) {
                            return
                        }
                    })
            }
        }
        userGroupsRef.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (snapshot.getValue(Int::class.java) == DatabaseOp.ADD.ordinal) {
                        userGroupsRef.child("static/${snapshot.key}").get().addOnCompleteListener {
                            unread[it.result.getValue(String::class.java)!!] = 0
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


        userContactsRef.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    when (snapshot.child("op").getValue(Int::class.java)) {
                        DatabaseOp.ADD.ordinal -> {
                            userContactsRef.child("static/${snapshot.key}").get()
                                .addOnCompleteListener {
                                    val contactUID = it.result.getValue(String::class.java)!!
                                    var username = "User"
                                    database.getReference("users/$contactUID/static/name").get()
                                        .addOnCompleteListener { name ->
                                            username = name.result.getValue(String::class.java)!!
                                        }
                                    //notify added contact
                                    userListeners[contactUID] =
                                        database.getReference("users/$contactUID/live").orderByKey()
                                            .limitToLast(1)
                                            .addChildEventListener(object : ChildEventListener {
                                                override fun onChildAdded(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    val key = snapshot.key!!
                                                    //notify contact details changed
                                                    when (snapshot.getValue(Int::class.java)) {
                                                        UserEdit.NAME.ordinal -> {
                                                            database.getReference("user/$contactUID/static/name")
                                                                .get()
                                                                .addOnCompleteListener { data ->
                                                                    val name =
                                                                        data.result.getValue(String::class.java)!!
                                                                    userNotifs.add(
                                                                        userNotif.setContentText(
                                                                            "$username -> $name"
                                                                        ).build()
                                                                    )
                                                                    if (userNotifs.size > 3) {
                                                                        userNotifs.removeAt(0)
                                                                    }
                                                                    for (i in 0..userNotifs.size) {
                                                                        NotificationManagerCompat.from(
                                                                            this@FeedService
                                                                        ).notify(
                                                                            userNotifIds[i],
                                                                            userNotifs[i]
                                                                        )
                                                                    }
                                                                    username = name
                                                                }
                                                        }
                                                        UserEdit.DESCRIPTION.ordinal -> {
                                                            database.getReference("user/$contactUID/static/description")
                                                                .get()
                                                                .addOnCompleteListener { data ->
                                                                    val description =
                                                                        data.result.getValue(String::class.java)
                                                                    userNotifs.add(
                                                                        userNotif.setContentText(
                                                                            description
                                                                        ).build()
                                                                    )
                                                                    if (userNotifs.size > 3) {
                                                                        userNotifs.removeAt(0)
                                                                    }
                                                                    for (i in 0..userNotifs.size) {
                                                                        NotificationManagerCompat.from(
                                                                            this@FeedService
                                                                        ).notify(
                                                                            userNotifIds[i],
                                                                            userNotifs[i]
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                        UserEdit.PROFILE.ordinal -> {
                                                            userNotifs.add(
                                                                userNotif.setContentText(
                                                                    "$username updated their profile"
                                                                ).build()
                                                            )
                                                            if (userNotifs.size > 3) {
                                                                userNotifs.removeAt(0)
                                                            }
                                                            for (i in 0..userNotifs.size) {
                                                                NotificationManagerCompat.from(
                                                                    this@FeedService
                                                                ).notify(
                                                                    userNotifIds[i],
                                                                    userNotifs[i]
                                                                )
                                                            }
                                                        }
                                                        UserEdit.DELETED.ordinal -> {
                                                            userNotifs.add(
                                                                userNotif.setContentText(
                                                                    "$username deleted their account"
                                                                ).build()
                                                            )
                                                            if (userNotifs.size > 3) {
                                                                userNotifs.removeAt(0)
                                                            }
                                                            for (i in 0..userNotifs.size) {
                                                                NotificationManagerCompat.from(
                                                                    this@FeedService
                                                                ).notify(
                                                                    userNotifIds[i],
                                                                    userNotifs[i]
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                override fun onChildChanged(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    return
                                                }

                                                override fun onChildRemoved(snapshot: DataSnapshot) {
                                                    return
                                                }

                                                override fun onChildMoved(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    return
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    return
                                                }
                                            })
                                }
                        }
                        DatabaseOp.DELETE.ordinal -> {
                            //notify deleted contact
                            val contactUID = snapshot.child("data").getValue(String::class.java)
                            val listener = userListeners.remove(contactUID)
                            if (listener != null) {
                                database.getReference("users/$contactUID/live")
                                    .removeEventListener(listener)
                            }
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
        userGroupsRef.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    when (snapshot.child("op").getValue(Int::class.java)) {
                        DatabaseOp.ADD.ordinal -> {
                            //notify group added
                            userGroupsRef.child("static/${snapshot.key}").get()
                                .addOnCompleteListener {
                                    val groupUID = it.result.getValue(String::class.java)!!
                                    var groupName = "Group"
                                    database.getReference("groups/$groupUID/static/name").get()
                                        .addOnCompleteListener { name ->
                                            groupName = name.result.getValue(String::class.java)!!
                                        }
                                    groupListeners[groupUID] =
                                        database.getReference("groups/$groupUID/live").orderByKey()
                                            .limitToLast(1)
                                            .addChildEventListener(object : ChildEventListener {
                                                override fun onChildAdded(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    //notify group details changed
                                                    when (snapshot.getValue(Int::class.java)) {
                                                        GroupEdit.NAME.ordinal -> {
                                                            database.getReference("user/$groupUID/static/name")
                                                                .get()
                                                                .addOnCompleteListener { data ->
                                                                    val name =
                                                                        data.result.getValue(String::class.java)
                                                                    groupNotifs.add(
                                                                        groupNotif.setContentText(
                                                                            "$groupName -> name"
                                                                        ).build()
                                                                    )
                                                                    if (groupNotifs.size > 3) {
                                                                        groupNotifs.removeAt(0)
                                                                    }
                                                                    for (i in 0..groupNotifs.size) {
                                                                        NotificationManagerCompat.from(
                                                                            this@FeedService
                                                                        ).notify(
                                                                            groupNotifIds[i],
                                                                            groupNotifs[i]
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                        GroupEdit.DESCRIPTION.ordinal -> {
                                                            database.getReference("user/$groupUID/static/description")
                                                                .get()
                                                                .addOnCompleteListener { data ->
                                                                    val description =
                                                                        data.result.getValue(String::class.java)
                                                                    groupNotifs.add(
                                                                        groupNotif.setContentText(
                                                                            description
                                                                        ).build()
                                                                    )
                                                                    if (groupNotifs.size > 3) {
                                                                        groupNotifs.removeAt(0)
                                                                    }
                                                                    for (i in 0..groupNotifs.size) {
                                                                        NotificationManagerCompat.from(
                                                                            this@FeedService
                                                                        ).notify(
                                                                            groupNotifIds[i],
                                                                            groupNotifs[i]
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                        GroupEdit.PROFILE.ordinal -> {
                                                            groupNotifs.add(
                                                                groupNotif.setContentText(
                                                                    "$groupName updated their profile"
                                                                ).build()
                                                            )
                                                            if (groupNotifs.size > 3) {
                                                                groupNotifs.removeAt(0)
                                                            }
                                                            for (i in 0..groupNotifs.size) {
                                                                NotificationManagerCompat.from(
                                                                    this@FeedService
                                                                ).notify(
                                                                    groupNotifIds[i],
                                                                    groupNotifs[i]
                                                                )
                                                            }

                                                        }
                                                        GroupEdit.DELETED.ordinal -> {
                                                            groupNotifs.add(
                                                                groupNotif.setContentText(

                                                                    "group $groupName was deleted"
                                                                ).build()
                                                            )
                                                            if (groupNotifs.size > 3) {
                                                                groupNotifs.removeAt(0)
                                                            }
                                                            for (i in 0..groupNotifs.size) {
                                                                NotificationManagerCompat.from(
                                                                    this@FeedService
                                                                ).notify(
                                                                    groupNotifIds[i],
                                                                    groupNotifs[i]
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                override fun onChildChanged(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    return
                                                }

                                                override fun onChildRemoved(snapshot: DataSnapshot) {
                                                    return
                                                }

                                                override fun onChildMoved(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    return
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    return
                                                }

                                            })
                                    groupMessageListeners[groupUID] =
                                        database.getReference("group_messages/$groupUID/live")
                                            .orderByKey().limitToLast(1)
                                            .addChildEventListener(object : ChildEventListener {
                                                override fun onChildAdded(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    //notify group messages changed
                                                    when (snapshot.getValue(Int::class.java)) {
                                                        DatabaseOp.ADD.ordinal -> {
                                                            database.getReference("user_messages/$groupUID/static/description")
                                                                .get()
                                                                .addOnCompleteListener { data ->
                                                                    val description =
                                                                        data.result.getValue(String::class.java)
                                                                    groupNotifs.add(
                                                                        groupNotif.setContentText(
                                                                            description
                                                                        ).build()
                                                                    )
                                                                    if (groupNotifs.size > 3) {
                                                                        groupNotifs.removeAt(0)
                                                                    }
                                                                    for (i in 0..groupNotifs.size) {
                                                                        NotificationManagerCompat.from(
                                                                            this@FeedService
                                                                        ).notify(
                                                                            groupNotifIds[i],
                                                                            groupNotifs[i]
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                    }
                                                }

                                                override fun onChildChanged(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    return
                                                }

                                                override fun onChildRemoved(snapshot: DataSnapshot) {
                                                    return
                                                }

                                                override fun onChildMoved(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    return
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    return
                                                }

                                            })
                                }
                        }
                        DatabaseOp.DELETE.ordinal -> {
                            //notify group deleted
                            val groupUID = snapshot.child("data").getValue(String::class.java)
                            val groupListener = groupListeners.remove(groupUID)
                            val groupMessageListener = groupMessageListeners.remove(groupUID)
                            if (groupListener != null) {
                                database.getReference("users/$groupUID/live")
                                    .removeEventListener(groupListener)
                            }
                            if (groupMessageListener != null) {
                                database.getReference("users/$groupUID/live")
                                    .removeEventListener(groupMessageListener)
                            }
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


        val builder = NotificationCompat.Builder(this, "com.picobyte.flantern")
            .setSmallIcon(R.drawable.document)
            .setContentTitle("My notification")
            .setContentText("Hello World!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true)
        /**
         * 3 notifs will be kept in memory and shifted downwards when new message is added
         * **/
        /**
         * Alright so the service will listen to all live references. first few stacks
         * of live references will also get from static, after which it will stop grabbing
         * from static. A notification will be displayed that when clicked, will check if
         * user is logged in, and then bring them to the respective page. if user is not logged
         * in, will send them to login page then to respective page.
         * ^ btw also remember to add the option to view other users profiles
         * ^ also rmb to make the screen non rotatable
         * ^ adding context menu for messages, allowing them to display the options:
         *  - Delete Message
         *  - Modify Message (should open dialog for this)
         *  - Reply
         *  or if the user is not you
         *  - Reply
         *  - Profile
         *  Usage graph is simple, just display data points ordered by key
         *
         **/

        return super.onStartCommand(intent, flags, startId)
    }
}