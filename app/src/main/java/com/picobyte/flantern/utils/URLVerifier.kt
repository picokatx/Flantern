package com.picobyte.flantern.utils

import java.net.MalformedURLException
import java.net.URL

fun isURLValid(s: String): Boolean {
    var url: URL
    try {
        url = URL(s)
    } catch (e: MalformedURLException) {
        return false
    }
    return url.protocol === "http:" || url.protocol === "https:"
}