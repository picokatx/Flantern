package com.picobyte.flantern.types

data class User(
    val name: String,
    val email: String,
    val description: String,
    val status: Status,
    val profile: String,
    val contacts: MessageTarget,
    val groups: MessageTarget,
    val threads: MessageTarget
)