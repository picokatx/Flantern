package com.picobyte.flantern.types

data class Embed(
    val type: Int? = null,
    val ref: String? = null
)
enum class EmbedType {
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT
}