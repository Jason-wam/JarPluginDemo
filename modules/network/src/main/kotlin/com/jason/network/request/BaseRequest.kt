package com.jason.network.request

import com.jason.network.OkHttpClientUtil
import com.jason.network.utils.CallManager
import com.jason.network.utils.UrlBuilder
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.nio.charset.Charset

open class BaseRequest<R> {
    private val urlBuilder = UrlBuilder()
    internal var builder: Request.Builder = Request.Builder()

    internal var onError: ((Throwable) -> Unit)? = null
    internal var onSuccess: ((R) -> Unit)? = null
    internal var onResponse: ((Response) -> Unit)? = null
    internal open var client = OkHttpClientUtil.baseClient.newBuilder().apply { CallManager.bind(this) }.build()

    /**
     * 用于设置请求参数的编码格式，解析返回数据时亦可用到
     *
     * 默认为 UTF-8
     */
    var charset: Charset = Charsets.UTF_8

    open val request: Request
        get() {
            return builder.build()
        }

    val url: String
        get() {
            return request.url.toString()
        }

    fun url(url: String) {
        urlBuilder.host(url)
        builder.url(urlBuilder.build())
    }

    fun tag(tag: Any) {
        builder.tag(tag)
    }

    fun path(path: String) {
        urlBuilder.path(path)
        builder.url(urlBuilder.build())
    }

    open fun param(name: String, value: String?) {
        value ?: return
        urlBuilder.param(name, value)
        builder.url(urlBuilder.build())
    }

    open fun param(name: String, value: Number?) {
        value ?: return
        urlBuilder.param(name, value)
        builder.url(urlBuilder.build())
    }

    open fun param(name: String, value: Boolean?) {
        value ?: return
        urlBuilder.param(name, value)
        builder.url(urlBuilder.build())
    }

    open fun setCharset(value: String) {
        charset = charset(value)
        urlBuilder.charset(value)
        builder.url(urlBuilder.build())
    }

    fun headers(headers: Headers) {
        builder.headers(headers)
    }

    fun header(name: String, value: String?) {
        value ?: return
        builder.header(name, value)
    }

    fun addHeader(name: String, value: String?) {
        value ?: return
        builder.addHeader(name, value)
    }

    /**
     * 修改当前Request的OkHttpClient配置, 不会影响全局默认的OkHttpClient
     */
    fun setClient(block: OkHttpClient.Builder.() -> Unit) {
        client = client.newBuilder().apply(block).apply { CallManager.bind(this) }.build()
    }

    /**
     * 追加修改当前Request的Builder配置
     */
    fun setBaseRequest(config: Request.Builder.() -> Unit) {
        builder.apply(config)
    }

    /**
     *  同步请求时不会执行，直接抛出异常
     */
    fun onError(onError: ((Throwable) -> Unit)) {
        this.onError = onError
    }

    fun onSuccess(onSuccess: ((R) -> Unit)) {
        this.onSuccess = onSuccess
    }

    /**
     * 在 [onError] 或 [onSuccess] 前回调 Response
     *
     * 注意：请勿在此处消费 Response.body，否则会导致[onSuccess]数据转换出错并执行[onError]
     */
    fun onResponse(onResponse: ((Response) -> Unit)) {
        this.onResponse = onResponse
    }
}