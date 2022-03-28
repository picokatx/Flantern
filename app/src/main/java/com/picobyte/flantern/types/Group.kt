package com.picobyte.flantern.types

data class Group(
    val name: String? = null,
    val description: String? = null,
    val profile: String? = null,
    val recent: Message? = null,
    val created: Long? = null,
    val messages: List<Message>? = null,
    val members: Members? = null,
    val threads: List<String>? = null
)