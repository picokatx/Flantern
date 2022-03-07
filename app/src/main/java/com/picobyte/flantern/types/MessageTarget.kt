package com.picobyte.flantern.types

data class MessageTarget(
    val pinned: List<String>,
    val muted: List<String>,
    val has: List<String>
)