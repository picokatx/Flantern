package com.picobyte.flantern

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.wifi.p2p.WifiP2pManager
import android.os.IBinder
import android.util.Log
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
import com.squareup.picasso.Picasso

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
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setGroup(ADD_GROUP_ID)
    val addUserNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("User was added")
        .setSmallIcon(R.drawable.profile)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setGroup(ADD_USER_ID)
    val groupNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("Group details were changed")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSmallIcon(R.drawable.group)
        .setGroup(GROUP_ID)
    val userNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("User details were changed")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSmallIcon(R.drawable.message)
        .setGroup(USER_ID)
    val groupMessageNotif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Flantern")
        .setContentText("Message was sent in group")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        return null
    }

    fun resetUnreadMessages(groupUID: String) {
        unread[groupUID] = 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        val userUID = intent!!.getStringExtra("user_uid")!!
        val userContactsRef = database.getReference("user_contacts/$userUID/has")
        val userGroupsRef = database.getReference("user_groups/$userUID/has")
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, "Flantern", importance)
            val soundAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            mChannel.description = "Flantern Channel"
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(mChannel)
        }
        Log.e("Flantern", "Service Started")
        userGroupsRef.child("static").get().addOnCompleteListener {
            it.result.children.forEach { child ->
                val groupUID = child.getValue(String::class.java)!!
                unread[groupUID] = 0
                database.getReference("group_messages/$groupUID/live").orderByKey().limitToLast(1)
                    .addChildEventListener(object : ChildEventListener {
                        val uid = groupUID
                        var fakeEventIntercept1 = false
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            if (!fakeEventIntercept1) {
                                fakeEventIntercept1 = true
                                return
                            }
                            if (snapshot.child("op")
                                    .getValue(Int::class.java) == DatabaseOp.ADD.ordinal
                            ) {
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
                var fakeEventIntercept2 = false
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (!fakeEventIntercept2) {
                        fakeEventIntercept2 = true
                        return
                    }
                    if (snapshot.child("op").getValue(Int::class.java) == DatabaseOp.ADD.ordinal) {
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
                var fakeEventIntercept3 = false
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (!fakeEventIntercept3) {
                        fakeEventIntercept3 = true
                        return
                    }
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
                                                var fakeEventIntercept4 = false
                                                override fun onChildAdded(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    if (!fakeEventIntercept4) {
                                                        fakeEventIntercept4 = true
                                                        return
                                                    }
                                                    val key = snapshot.key!!
                                                    //notify contact details changed
                                                    when (snapshot.child("op")
                                                        .getValue(Int::class.java)) {
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
                                                                    for (i in 0 until userNotifs.size) {
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
                                                                    for (i in 0 until userNotifs.size) {
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
                                                            for (i in 0 until userNotifs.size) {
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
                                                            for (i in 0 until userNotifs.size) {
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
        userGroupsRef.child("static").get().addOnCompleteListener {
            it.result.children.forEach { child ->

            }
        }

        userGroupsRef.child("live").orderByKey().limitToLast(1)
            .addChildEventListener(object : ChildEventListener {
                var fakeEventIntercept5 = false
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (!fakeEventIntercept5) {
                        fakeEventIntercept5 = true
                        return
                    }
                    when (snapshot.child("op").getValue(Int::class.java)) {
                        DatabaseOp.ADD.ordinal -> {
                            //notify group added
                            Log.e("Flantern", "Group Added")
                            userGroupsRef.child("static/${snapshot.key}").get()
                                .addOnCompleteListener {
                                    val groupUID = it.result.getValue(String::class.java)!!
                                    var groupName = "Group"
                                    Log.e("Flantern grouo", groupUID)
                                    database.getReference("groups/$groupUID/static/name").get()
                                        .addOnCompleteListener { name ->
                                            groupName = name.result.getValue(String::class.java)!!
                                        }
                                    groupListeners[groupUID] =
                                        database.getReference("groups/$groupUID/live").orderByKey()
                                            .limitToLast(1)
                                            .addChildEventListener(object : ChildEventListener {
                                                var fakeEventIntercept6 = false
                                                override fun onChildAdded(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    if (!fakeEventIntercept6) {
                                                        fakeEventIntercept6 = true
                                                        return
                                                    }
                                                    //notify group details changed
                                                    when (snapshot.child("op")
                                                        .getValue(Int::class.java)) {
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
                                                                    for (i in 0 until groupNotifs.size) {
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
                                                                    for (i in 0 until groupNotifs.size) {
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
                                                            for (i in 0 until groupNotifs.size) {
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
                                                            for (i in 0 until groupNotifs.size) {
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
                                    Log.e("Flantern listeners", groupMessageListeners.size.toString())
                                    groupMessageListeners[groupUID] =
                                        database.getReference("group_messages/$groupUID/live")
                                            .orderByKey().limitToLast(1)
                                            .addChildEventListener(object : ChildEventListener {
                                                var fakeEventIntercept7 = false
                                                override fun onChildAdded(
                                                    snapshot: DataSnapshot,
                                                    previousChildName: String?
                                                ) {
                                                    if (!fakeEventIntercept7) {
                                                        fakeEventIntercept7 = true
                                                        return
                                                    }
                                                    Log.e("Flantern", "Hello I m running")
                                                    //notify group messages changed
                                                    when (snapshot.child("op")
                                                        .getValue(Int::class.java)) {
                                                        DatabaseOp.ADD.ordinal -> {
                                                            Log.e("Flantern", "Message Sent")
                                                            database.getReference("group_messages/$groupUID/static/${snapshot.key}/content")
                                                                .get()
                                                                .addOnCompleteListener { data ->
                                                                    val description =
                                                                        data.result.getValue(String::class.java)!!
                                                                    Log.e("Flantern", description)
                                                                    groupMessageNotifs.add(
                                                                        groupMessageNotif.setContentText(
                                                                            description
                                                                        ).build()
                                                                    )
                                                                    if (groupMessageNotifs.size > 3) {
                                                                        groupMessageNotifs.removeAt(
                                                                            0
                                                                        )
                                                                    }
                                                                    Log.e("Flantern", groupMessageNotifs.size.toString())
                                                                    for (i in 0 until groupMessageNotifs.size) {
                                                                        notificationManager.notify(
                                                                            groupMessageNotifIds[i],
                                                                            groupMessageNotifs[i]
                                                                        )
                                                                        Log.e("Flantern notif dispatch", i.toString())
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
                                    Log.e("Flantern listeners", groupMessageListeners.size.toString())
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