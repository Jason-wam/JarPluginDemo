package com.jason.network.extension

import com.jason.network.error.CallCanceledException
import okhttp3.Call
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.math.BigInteger
import java.security.MessageDigest


fun InputStream.copyTo(
    outStream: OutputStream, progress: (percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit
) {
    copyTo(contentLength = -1, outStream, progress)
}

/**
 * 将当前输入流复制到指定的输出流，并提供进度回调。
 *
 * 此函数的主要目的是在将数据从一个流复制到另一个流时，提供进度更新的功能。
 * 它可以用于诸如文件传输或备份的场景，让用户了解操作的进度。
 *
 * @param outStream 要将数据复制到的输出流。
 * @param progress 一个接收进度更新的回调函数，进度以0到100的整数表示。
 * @return 返回已复制的字节数。
 */
fun InputStream.copyTo(
    contentLength: Long = -1,
    outStream: OutputStream,
    progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): Long {
    var bytesCopied: Long = 0
    val totalBytes = if (this is FileInputStream) {
        channel.size()
    } else {
        if (contentLength > 0) contentLength else available().toLong()
    }

    val buffer = ByteArray(4096)
    var bytes = read(buffer)
    while (bytes >= 0) {
        outStream.write(buffer, 0, bytes)
        bytesCopied += bytes
        progress?.let {
            val percent = if (totalBytes > 0) bytesCopied.toFloat() / totalBytes.toFloat() * 100 else 0f
            it.invoke(percent, bytesCopied, totalBytes)
        }
        bytes = read(buffer)
    }
    return bytesCopied
}

fun InputStream.copyTo(
    contentLength: Long = -1,
    file: RandomAccessFile,
    progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): Long {
    var bytesCopied: Long = 0
    val totalBytes = if (this is FileInputStream) {
        channel.size()
    } else {
        if (contentLength > 0) contentLength else available().toLong()
    }

    val buffer = ByteArray(4096)
    var bytes = read(buffer)
    while (bytes >= 0) {
        file.write(buffer, 0, bytes)
        bytesCopied += bytes
        progress?.let {
            val percent = if (totalBytes > 0) bytesCopied.toFloat() / totalBytes.toFloat() * 100 else 0f
            it.invoke(percent, bytesCopied, totalBytes)
        }
        bytes = read(buffer)
    }
    return bytesCopied
}

fun FileInputStream.md5(
    call: Call? = null, progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): String {
    return sum(call, MessageDigest.getInstance("MD5"), progress)
}

fun FileInputStream.sha1(
    call: Call? = null, progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): String {
    return sum(call, MessageDigest.getInstance("SHA-1"), progress)
}

fun FileInputStream.sha256(
    call: Call? = null, progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): String {
    return sum(call, MessageDigest.getInstance("SHA-256"), progress)
}

fun InputStream.md5(
    call: Call? = null,
    contentLength: Long = -1,
    progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): String {
    return sum(call, MessageDigest.getInstance("MD5"), contentLength, progress)
}

fun InputStream.sha1(
    call: Call? = null,
    contentLength: Long = -1,
    progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): String {
    return sum(call, MessageDigest.getInstance("SHA-1"), contentLength, progress)
}

fun InputStream.sha256(
    call: Call? = null,
    contentLength: Long = -1,
    progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): String {
    return sum(call, MessageDigest.getInstance("SHA-256"), contentLength, progress)
}

fun InputStream.sum(
    call: Call? = null,
    digest: MessageDigest,
    contentLength: Long = -1,
    progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): String {
    val buffer = ByteArray(4096)
    var bytesRead: Int
    var totalBytesCopied: Long = 0
    val totalBytes = if (this is FileInputStream) {
        channel.size()
    } else {
        if (contentLength > 0) contentLength else available().toLong()
    }

    while (read(buffer).also { bytesRead = it } != -1) {
        if (call?.isCanceled() == true) {
            throw CallCanceledException("Call canceled!")
        }
        digest.update(buffer)
        totalBytesCopied += bytesRead
        progress?.let {
            val percent = if (totalBytes > 0) totalBytesCopied.toFloat() / totalBytes.toFloat() * 100 else 0f
            it.invoke(percent, totalBytesCopied, totalBytes)
        }
    }

    // 计算 MD5 值并转换为十六进制字符串
    val checksum = BigInteger(1, digest.digest()).toString(16)
    return checksum.padStart(32, '0')
}

fun FileInputStream.sum(
    call: Call? = null,
    digest: MessageDigest,
    progress: ((percent: Float, bytesCopied: Long, totalBytes: Long) -> Unit)? = null
): String {
    val buffer = ByteArray(4096)
    var bytesRead: Int
    var totalBytesCopied: Long = 0
    val totalBytes = channel.size() // 获取文件总字节数

    while (read(buffer).also { bytesRead = it } != -1) {
        if (call?.isCanceled() == true) {
            throw CallCanceledException("Call canceled!")
        }
        digest.update(buffer)
        totalBytesCopied += bytesRead
        progress?.let {
            val percent = if (totalBytes > 0) totalBytesCopied.toFloat() / totalBytes.toFloat() * 100 else 0f
            it.invoke(percent, totalBytesCopied, totalBytes)
        }
    }

    // 计算 MD5 值并转换为十六进制字符串
    val checksum = BigInteger(1, digest.digest()).toString(16)
    return checksum.padStart(32, '0')
}