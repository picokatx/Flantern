package com.picobyte.flantern.types

data class Message(
    val user: String? = null,
    val content: String? = null,
    val timestamp: Long? = null,
    val replying: String? = null,
    val forwarded: Boolean? = null
)