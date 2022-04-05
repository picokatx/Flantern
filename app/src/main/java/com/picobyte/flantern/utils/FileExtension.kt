package com.picobyte.flantern.utils

import android.webkit.MimeTypeMap

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
//https://stackoverflow.com/questions/9758151/get-the-file-extension-from-images-picked-from-gallery-or-camera-as-string

fun getMimeType(context: Context, uri: Uri): String? {

    //Check uri format to avoid null
    val extension: String? = if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        //If scheme is a content
        val mime = MimeTypeMap.getSingleton()
        mime.getExtensionFromMimeType(context.contentResolver.getType(uri))
    } else {
        //If scheme is a File
        //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
        MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(uri.path)).toString())
    }
    return extension
}
