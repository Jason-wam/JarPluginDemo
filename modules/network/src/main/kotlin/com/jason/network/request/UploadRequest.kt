package com.jason.network.request

import com.jason.network.OkHttpClientUtil
import com.jason.network.utils.MediaConst
import com.jason.network.ProgressRequestBody
import com.jason.network.converter.ResponseConverter
import com.jason.network.extension.toRequestBody
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

@Suppress("unused")
class UploadRequest<R> : BaseRequest<R>() {
    internal var converter: ResponseConverter<R>? = null

    internal var onProgress: ((percent: Float, uploadedBytes: Long, totalBytes: Long) -> Unit)? = null
    override var client = OkHttpClientUtil.longClient

    /**
     * 请求体
     */
    internal var body: RequestBody? = null

    /**
     * multipart请求体
     * 主要存放文件/IO流
     */
    internal var partBody = MultipartBody.Builder()

    /**
     * 表单请求体
     * 当你设置`partBody`后当前表单请求体中的所有参数都会被存放到partBody中
     */
    internal var formBody = FormBody.Builder(charset)

    override fun setCharset(value: String) {
        super.setCharset(value)
        val form = formBody.build()
        formBody = if (form.size == 0) {
            FormBody.Builder(charset)
        } else {
            FormBody.Builder(charset).apply {
                for (i in 0 until form.size) {
                    param(form.name(i), form.value(i))
                }
            }
        }
    }

    /**
     * multipart请求体的媒体类型
     */
    internal var mediaType: MediaType = MediaConst.FORM

    override val request: Request
        get() {
            val body = if (body != null) body else {
                val form = formBody.build()
                try {
                    partBody.build()
                    for (i in 0 until form.size) {
                        val name = form.name(i)
                        val value = form.value(i)
                        partBody.addFormDataPart(name, value)
                    }
                    partBody.setType(mediaType).build()
                } catch (e: IllegalStateException) {
                    form
                }
            }
            if (body != null) {
                builder.post(ProgressRequestBody(body).apply {
                    setProgressListener { percent, uploadedBytes, totalBytes ->
                        onProgress?.invoke(percent, uploadedBytes, totalBytes)
                    }
                })
            }
            return builder.build()
        }

    /**
     * 不同于普通请求，这里的方法为添加表单参数
     *
     * 添加到 FormBody
     */
    override fun param(name: String, value: String?) {
        value ?: return
        formBody.add(name, value)
    }

    /**
     * 添加到 FormBody
     */
    fun param(name: String, value: String, encoded: Boolean) {
        if (encoded) {
            formBody.addEncoded(name, value)
        } else {
            formBody.add(name, value)
        }
    }

    /**
     * 添加到 FormBody
     */
    override fun param(name: String, value: Number?) {
        value ?: return
        formBody.add(name, value.toString())
    }

    /**
     * 添加到 FormBody
     */
    override fun param(name: String, value: Boolean?) {
        value ?: return
        formBody.add(name, value.toString())
    }

    /**
     * 添加到 FormDataPart
     */
    fun param(name: String, value: RequestBody?) {
        value ?: return
        partBody.addFormDataPart(name, null, value)
    }

    /**
     * 添加到 FormDataPart
     */
    fun param(name: String, filename: String?, value: RequestBody?) {
        value ?: return
        partBody.addFormDataPart(name, filename, value)
    }

    /**
     * 添加到 FormDataPart
     */
    fun param(name: String, value: ByteString?) {
        value ?: return
        partBody.addFormDataPart(name, null, value.toRequestBody())
    }

    /**
     * 添加到 FormDataPart
     */
    fun param(name: String, value: ByteArray?) {
        value ?: return
        partBody.addFormDataPart(name, null, value.toRequestBody())
    }

    /**
     * 添加到 FormDataPart
     */
    fun param(name: String, value: File?) {
        value ?: return
        partBody.addFormDataPart(name, value.name, value.toRequestBody())
    }

    /**
     * 添加到 FormDataPart
     */
    fun param(name: String, value: List<File?>?) {
        value?.forEach { file ->
            param(name, file)
        }
    }

    /**
     * 添加到 FormDataPart
     */
    fun param(name: String, fileName: String?, value: File?) {
        partBody.addFormDataPart(name, fileName, value?.toRequestBody() ?: return)
    }

    /**
     * 添加到 partBody
     */
    fun param(body: MultipartBody.Part) {
        partBody.addPart(body)
    }

    /**
     * 添加Json为请求体::body
     */
    fun json(body: JSONObject?) {
        this.body = body?.toString()?.toRequestBody(MediaConst.JSON)
    }

    /**
     * 添加Json为请求体::body
     */
    fun json(body: JSONArray?) {
        this.body = body?.toString()?.toRequestBody(MediaConst.JSON)
    }

    /**
     * 添加Json为请求体::body
     */
    fun json(body: String?) {
        this.body = body?.toRequestBody(MediaConst.JSON)
    }

    /**
     * 添加Json为请求体::body
     */
    fun json(body: Map<String, Any?>?) {
        this.body = JSONObject(body ?: return).toString().toRequestBody(MediaConst.JSON)
    }

    /**
     * 添加Json对象为请求体::body
     */
    fun json(vararg body: Pair<String, Any?>) {
        this.body = JSONObject(body.toMap()).toString().toRequestBody(MediaConst.JSON)
    }

    fun setBody(body: RequestBody): UploadRequest<R> {
        builder.post(ProgressRequestBody(body).apply {
            setProgressListener { percent, uploadedBytes, totalBytes ->
                onProgress?.invoke(percent, uploadedBytes, totalBytes)
            }
        })
        return this
    }

    fun setConverter(converter: ResponseConverter<R>): UploadRequest<R> {
        this.converter = converter
        return this
    }


    fun onProgress(onProgress: ((percent: Float, uploadedBytes: Long, totalBytes: Long) -> Unit)): UploadRequest<R> {
        this.onProgress = onProgress
        return this
    }
}