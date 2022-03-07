package com.picobyte.flantern.types

data class Thread(
    val name: String,
    val description: String,
    val parent: String,
    val messages: List<Message>
)