package com.picobyte.flantern.ext

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class Launchers(val context: AppCompatActivity) {
    lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    fun registerGallery(callback: ActivityResultCallback<ActivityResult>) {
        galleryLauncher = context.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(), context.activityResultRegistry, callback
        )
    }
}