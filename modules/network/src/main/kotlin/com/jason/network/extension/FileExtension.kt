package com.jason.network.extension

import okhttp3.Call
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.File

fun File.verifyMD5(
    call: Call? = null,
    md5: String,
    onVerifyFile: ((percent: Float, totalCopied: Long, totalSize: Long) -> Unit)? = null
): Boolean {
    return inputStream().use { it.md5(call, onVerifyFile) }.equals(md5, ignoreCase = true)
}

fun File.verifySHA1(
    call: Call? = null,
    sha1: String,
    onVerifyFile: ((percent: Float, totalCopied: Long, totalSize: Long) -> Unit)? = null
): Boolean {
    return inputStream().use { it.sha1(call, onVerifyFile) }.equals(sha1, ignoreCase = true)
}

fun File.verifyShA256(
    call: Call? = null,
    sha256: String,
    onVerifyFile: ((percent: Float, totalCopied: Long, totalSize: Long) -> Unit)? = null
): Boolean {
    return inputStream().use { it.sha256(call, onVerifyFile) }.equals(sha256, ignoreCase = true)
}

/**
 * 返回文件的MediaType值, 如果不存在返回null
 */
fun File.guessMediaType(): MediaType? {
    return ClassLoader.getSystemClassLoader().getResourceAsStream("MimeType.json")?.bufferedReader()?.use {
        JSONObject(it.readText())
    }?.let {
        if (it.has(nameWithoutExtension)) {
            it.getString(nameWithoutExtension).toMediaTypeOrNull()
        } else {
            null
        }
    }
}

/**
 * 创建File的RequestBody
 * @param contentType 如果为null则通过判断扩展名来生成MediaType
 */
fun File.toRequestBody(contentType: MediaType? = null): RequestBody {
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return contentType ?: guessMediaType()
        }

        override fun contentLength() = length()

        override fun writeTo(sink: BufferedSink) {
            source().use { source ->
                sink.writeAll(source)
            }
        }
    }
}