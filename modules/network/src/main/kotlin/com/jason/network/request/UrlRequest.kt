package com.jason.network.request

import com.jason.network.cache.CacheMode
import com.jason.network.cache.CacheValidDuration
import com.jason.network.converter.ResponseConverter
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.RequestBody

@Suppress("unused")
class UrlRequest<R> : BaseRequest<R>() {
    internal var cacheMode: CacheMode = CacheMode.ONLY_NETWORK
    internal var cacheValidDuration: Long = CacheValidDuration.FOREVER
    internal var converter: ResponseConverter<R>? = null
    internal var standAloneCacheKay: String? = null

    fun setCache(mode: CacheMode, duration: Long = CacheValidDuration.FOREVER): UrlRequest<R> {
        this.cacheMode = mode
        this.cacheValidDuration = duration
        return this
    }

    fun post(body: RequestBody) {
        builder.post(body)
    }

    /**
     * 添加Form请求体
     */
    fun postForm(form: FormBody.Builder.() -> Unit) {
        builder.post(FormBody.Builder(charset).apply(form).build())
    }

    /**
     * 添加Multipart请求体
     */
    fun postParts(form: MultipartBody.Builder.() -> Unit) {
        builder.post(MultipartBody.Builder().apply(form).build())
    }

    fun put(body: RequestBody) {
        builder.put(body)
    }

    fun delete(body: RequestBody?) {
        builder.delete(body)
    }

    fun patch(body: RequestBody) {
        builder.patch(body)
    }

    fun get() {
        builder.get()
    }

    fun head() {
        builder.head()
    }

    fun method(method: Method, body: RequestBody? = null) {
        builder.method(method.name, body)
    }

    fun setCacheMode(cacheMode: CacheMode) {
        this.cacheMode = cacheMode
    }

    /**
     * 设置缓存Key，默认为 url + method + headers
     *
     * 通常情况下无需设置此参数，但在某些情况下，例如：
     *
     * 1. 如果Method为 POST，则可能根据需要追加使用 body 的 唯一标识
     *
     * 2. 请求的url为动态的，例如：url为：/api/info/{id}，id为动态参数，
     *    此时，默认缓存Key无法满足需求，此时，需要考虑手动设置缓存Key
     *
     */
    fun setCacheKey(key: String) {
        this.standAloneCacheKay = key
    }

    /**
     * 设置缓存有效时长
     * @param duration 单位：毫秒
     */
    fun setCacheValidDuration(duration: Long) {
        this.cacheValidDuration = duration
    }

    fun setConverter(converter: ResponseConverter<R>) {
        this.converter = converter
    }
}