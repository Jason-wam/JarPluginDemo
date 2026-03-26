package com.jason.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.IOException
import okio.Sink
import okio.buffer

class ProgressRequestBody(val body: RequestBody) : RequestBody() {
    private var writeByteCount = 0L
    private val contentLength by lazy { body.contentLength() }
    private var progressListener: ((percent: Float, downloadBytes: Long, totalBytes: Long) -> Unit)? = null

    fun setProgressListener(progressListener: ((percent: Float, uploadedBytes: Long, totalBytes: Long) -> Unit)): ProgressRequestBody {
        this.progressListener = progressListener
        return this
    }

    override fun contentLength(): Long {
        return contentLength
    }

    override fun contentType(): MediaType? {
        return body.contentType()
    }

    override fun writeTo(sink: BufferedSink) {
        if (sink is Buffer || sink.toString()
                .contains("com.android.tools.profiler.support.network.HttpTracker\$OutputStreamTracker")
        ) { //Android Studio用于追踪网络请求的内部类
            body.writeTo(sink)
        } else {
            val bufferedSink: BufferedSink = sink.toProgress().buffer()
            body.writeTo(bufferedSink)
            bufferedSink.closeQuietly()
            if (contentLength == -1L) {
                progressListener?.invoke(100f, writeByteCount, contentLength)
            }
        }
    }

    private fun Sink.toProgress() = object : ForwardingSink(this) {
        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            progressListener?.let {
                writeByteCount += byteCount
                if (contentLength == -1L) {
                    it.invoke(0f, writeByteCount, contentLength)
                } else {
                    val percent = writeByteCount / contentLength.toFloat() * 100
                    it.invoke(percent, writeByteCount, contentLength)
                }
            }
        }
    }
}