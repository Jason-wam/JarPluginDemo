package com.jason.network.cache

import com.jakewharton.disklrucache.DiskLruCache
import com.jason.network.extension.cacheKey
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import okio.BufferedSource
import okio.buffer
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

internal object ResponseCache {
    private var cache: DiskLruCache? = null
    private const val CACHE_KEY_INFO = 0
    private const val CACHE_KEY_BODY = 1
    private const val CACHE_KEY_TIME = 2

    fun init(dir: File) {
        try {
            dir.mkdirs()
            cache = DiskLruCache.open(dir, 1, 3, 1024 * 1024 * 1024 * 3L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun put(cacheKey: String? = null, request: Request, response: Response, validDuration: Long): Response {
        if (validDuration == CacheValidDuration.NEVER) return response
        try {
            val key = cacheKey ?: request.cacheKey
            cache ?: return response
            cache?.remove(key) //覆盖数据

            val editor = cache?.edit(key)
            editor?.set(CACHE_KEY_INFO, response.info().toString())
            editor?.set(CACHE_KEY_TIME, System.currentTimeMillis().toString())

            val body = response.body ?: return response
            val outputStream = editor?.newOutputStream(CACHE_KEY_BODY)
            if (outputStream == null) {
                editor?.abort()
                return response
            } else {
                return if (body.source().inputStream().copyTo(outputStream) <= 0) {
                    outputStream.closeQuietly()
                    editor.abort()
                    response
                } else {
                    outputStream.closeQuietly()
                    editor.commit()
                    response.newBuilder().let {
                        val snapshot = cache?.get(key)
                        if (snapshot != null) {
                            it.body(
                                CacheResponseBody(
                                    snapshot, response.headers["Content-Type"], response.headers["Content-Length"]
                                )
                            )
                        }
                        it.build()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return response
        }
    }

    fun get(cacheKey: String? = null, request: Request, validDuration: Long = CacheValidDuration.FOREVER): Response? {
        try {
            cache ?: return null
            val key = cacheKey ?: request.cacheKey
            val snapshot = cache?.get(key) ?: return null
            val info = snapshot.getString(CACHE_KEY_INFO)?.let { JSONObject(it) } ?: return null
            val time = snapshot.getString(CACHE_KEY_TIME)?.toLongOrNull() ?: 0
            if (validDuration == CacheValidDuration.FOREVER || validDuration > System.currentTimeMillis() - time) {
                val headers = Headers.Builder()
                val array = info.getJSONArray("responseHeaders")
                for (i in 0 until array.length()) {
                    val header = array.getJSONObject(i)
                    headers.add(header.getString("name"), header.getString("value"))
                }

                return Response.Builder().let {
                    it.request(request)
                    it.code(info.getInt("code"))
                    it.headers(headers.build())
                    it.header("Is-From-Cache", "true")
                    it.message(info.getString("message"))
                    it.protocol(info.getString("protocol").let {
                        Protocol.valueOf(it)
                    })
                    it.sentRequestAtMillis(info.getLong("sentRequestAtMillis"))
                    it.receivedResponseAtMillis(info.getLong("receivedResponseAtMillis"))

                    it.body(
                        CacheResponseBody(
                            snapshot, headers["Content-Type"], headers["Content-Length"]
                        )
                    )
                    it.build()
                }
            }
            return null
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    private fun Response.info(): JSONObject {
        return JSONObject().apply {
            put("code", code)
            put("method", request.method)
            put("message", message)
            put("protocol", protocol.name)
            put("sentRequestAtMillis", sentRequestAtMillis)
            put("receivedResponseAtMillis", receivedResponseAtMillis)
            put("responseHeaders", JSONArray().apply {
                for (i in 0 until headers.size) {
                    put(JSONObject().apply {
                        put("name", headers.name(i))
                        put("value", headers.value(i))
                    })
                }
            })
        }
    }

    private class CacheResponseBody(
        val snapshot: DiskLruCache.Snapshot, val contentType: String?, val contentLength: String?
    ) : ResponseBody() {
        override fun contentType(): MediaType? {
            return contentType?.toMediaTypeOrNull()
        }

        override fun contentLength(): Long {
            return contentLength?.toLong() ?: -1
        }

        override fun source(): BufferedSource {
            return snapshot.getInputStream(CACHE_KEY_BODY).source().buffer()
        }

        override fun close() {
            super.close()
            snapshot.close()
        }
    }
}