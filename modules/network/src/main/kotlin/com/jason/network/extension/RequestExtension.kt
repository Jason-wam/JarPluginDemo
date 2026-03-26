package com.jason.network.extension

import okhttp3.Request
import okhttp3.internal.closeQuietly
import okio.Buffer
import okio.BufferedSink
import okio.ByteString.Companion.encodeUtf8

inline val Request.cacheKey: String
    get() {
        return cacheKey()
    }

/**
 * 带盐的缓存key
 */
fun Request.cacheKey(salt: String? = null): String {
    return buildString {
        append(url.toString())
        append(method)
        append(headers.joinToString(","))
        if (method == "POST") {
            append(body?.contentType())
            append(body?.contentLength())
            val buffer = Buffer()
            body?.writeTo(buffer)
            append(buffer.readString(Charsets.UTF_8))
            buffer.closeQuietly()
        }
        if (salt != null) {
            append(salt)
        }
    }.encodeUtf8().md5().hex()
}