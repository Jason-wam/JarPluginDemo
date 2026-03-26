package com.jason.network.extension

import okhttp3.Response
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * 获取响应的文件名
 */
fun Response.fileName(defaultName: String = ""): String {
    val disposition = header("Content-Disposition")
    if (disposition != null) {
        disposition.substringAfter("filename=", "").takeUnless { it.isBlank() }?.let { return it }
        disposition.substringAfter("filename*=", "").trimStart(*"UTF-8''".toCharArray()).takeUnless { it.isBlank() }
            ?.let { return it }
    }

    var fileName: String = request.url.pathSegments.last().substringBefore("?")
    fileName = if (fileName.isBlank()) {
        defaultName.ifBlank {
            "unknown_${System.currentTimeMillis()}"
        }
    } else {
        try {
            URLDecoder.decode(fileName, "UTF-8")
        } catch (_: Exception) {
            fileName
        }
    }
    return fileName
}

fun Response.readString(charset: String = "utf-8"): String? {
    return body?.source()?.readString(charset(charset))
}

fun Response.readString(charset: Charset = Charsets.UTF_8): String? {
    return body?.source()?.readString(charset)
}

inline val Response.isFromCache: Boolean
    get() {
        return header("Is-From-Cache") == "true"
    }