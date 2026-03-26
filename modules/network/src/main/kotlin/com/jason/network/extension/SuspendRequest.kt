package com.jason.network.extension

import com.jason.network.OkHttpClientUtil.cancelByTag
import com.jason.network.OkHttpClientUtil.downloadAsync
import com.jason.network.OkHttpClientUtil.enqueue
import com.jason.network.OkHttpClientUtil.uploadAsync
import com.jason.network.request.DownloadRequest
import com.jason.network.request.UploadRequest
import com.jason.network.request.UrlRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend inline fun <reified R> suspendCancellableRequest(crossinline requestConfig: UrlRequest<R>.() -> Unit) =
    suspendCancellableCoroutine { continuation ->
        val request = UrlRequest<R>().apply(requestConfig)
        val requestTag = request.request.tag() ?: System.currentTimeMillis()
        if (request.request.tag() == null) {
            request.tag(requestTag)
        }
        request.onError {
            if (continuation.isActive) {
                continuation.resume(Result.failure(it))
            }
        }
        request.onSuccess {
            if (continuation.isActive) {
                continuation.resume(Result.success(it))
            }
        }
        enqueue(request, R::class)
        continuation.invokeOnCancellation {
            cancelByTag(requestTag)
        }
    }

suspend inline fun suspendCancellableDownload(crossinline requestConfig: DownloadRequest.() -> Unit) =
    suspendCancellableCoroutine { continuation ->
        val request = DownloadRequest().apply(requestConfig)
        val requestTag = request.request.tag() ?: System.currentTimeMillis()
        if (request.request.tag() == null) {
            request.tag(requestTag)
        }
        request.onError {
            if (continuation.isActive) {
                continuation.resume(Result.failure(it))
            }
        }
        request.onSuccess {
            if (continuation.isActive) {
                continuation.resume(Result.success(it))
            }
        }
        downloadAsync(request)
        continuation.invokeOnCancellation {
            cancelByTag(requestTag)
        }
    }

suspend inline fun <reified R> suspendCancellableUpload(crossinline requestConfig: UploadRequest<R>.() -> Unit) =
    suspendCancellableCoroutine { continuation ->
        val request = UploadRequest<R>().apply(requestConfig)
        val requestTag = request.request.tag() ?: System.currentTimeMillis()
        if (request.request.tag() == null) {
            request.tag(requestTag)
        }
        request.onError {
            if (continuation.isActive) {
                continuation.resume(Result.failure(it))
            }
        }
        request.onSuccess {
            if (continuation.isActive) {
                continuation.resume(Result.success(it))
            }
        }
        uploadAsync(request, R::class)
        continuation.invokeOnCancellation {
            cancelByTag(requestTag)
        }
    }