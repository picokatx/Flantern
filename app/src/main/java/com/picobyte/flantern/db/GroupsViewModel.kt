package com.picobyte.flantern.db

import com.google.firebase.database.DataSnapshot
import androidx.lifecycle.LiveData
import com.google.firebase.database.FirebaseDatabase
import androidx.lifecycle.ViewModel


class GroupsViewModel : ViewModel() {
    fun getGroups(): LiveData<DataSnapshot> {
        return FirebaseQueryLiveData(FirebaseDatabase.getInstance().getReference("/groups"))
    }

    fun getGroup(key: String): LiveData<DataSnapshot> {
        return FirebaseQueryLiveData(FirebaseDatabase.getInstance().getReference("/groups/$key"))
    }
}