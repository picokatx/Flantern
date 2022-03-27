package com.picobyte.flantern.types

import android.graphics.Bitmap

data class Message(
    val user: String? = null,
    val content: String? = null,
    val timestamp: Long? = null,
    val replying: String? = null,
    val forwarded: Boolean? = null,
    val embed: Embed? = null,
    var embedRaw: Bitmap? = null
)