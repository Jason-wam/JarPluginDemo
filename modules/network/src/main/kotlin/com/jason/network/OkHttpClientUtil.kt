package com.jason.network

import com.jason.network.cache.CacheMode
import com.jason.network.cache.ResponseCache
import com.jason.network.converter.JSONArrayConverter
import com.jason.network.converter.JSONObjectConverter
import com.jason.network.converter.NoConverter
import com.jason.network.converter.ResponseConverter
import com.jason.network.converter.StringConverter
import com.jason.network.error.CallCanceledException
import com.jason.network.error.FileVerificationException
import com.jason.network.extension.cacheKey
import com.jason.network.extension.copyTo
import com.jason.network.extension.fileName
import com.jason.network.extension.trustSSLCertificate
import com.jason.network.extension.verifyMD5
import com.jason.network.extension.verifySHA1
import com.jason.network.extension.verifyShA256
import com.jason.network.request.DownloadRequest
import com.jason.network.request.UploadRequest
import com.jason.network.request.UrlRequest
import com.jason.network.utils.CallManager
import com.jason.network.utils.OkhttpLogger
import okhttp3.*
import okio.use
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.reflect.KClass

@Suppress("unused")
object OkHttpClientUtil {
    /**
     * 默认的全局client，用于执行普通请求
     */
    var baseClient: OkHttpClient = OkHttpClient.Builder().apply {
        trustSSLCertificate()
        followRedirects(true)
        followSslRedirects(true)
        hostnameVerifier { _, _ -> true }
        retryOnConnectionFailure(true)
        callTimeout(60, TimeUnit.SECONDS)
        connectTimeout(60, TimeUnit.SECONDS)
        readTimeout(60, TimeUnit.SECONDS)
        writeTimeout(60, TimeUnit.SECONDS)
        dispatcher(Dispatcher().apply {
            maxRequests = 64
        })
        CallManager.bind(this)
    }.build()

    /**
     * 长连接的全局client，用于下载文件等耗时较长的操作
     */
    val longClient by lazy {
        baseClient.newBuilder().apply {
            callTimeout(10, TimeUnit.DAYS)
            connectTimeout(10, TimeUnit.DAYS)
            readTimeout(10, TimeUnit.DAYS)
            writeTimeout(10, TimeUnit.DAYS)
        }.build()
    }

    /**
     * 默认的转换器列表，用于解析响应
     */
    val converters = ArrayList<ResponseConverter<*>>().apply {
        add(StringConverter())
        add(JSONObjectConverter())
        add(JSONArrayConverter())
        add(NoConverter())
    }

    /**
     * 设置全局的client，用于执行特殊请求
     *
     * @param config OkHttpClient的配置块，用于设置client的参数，如超时时间、连接池等
     *
     * 此方法不会覆盖之前的client配置，而是将新的配置块合并到旧的配置中
     *
     * 每个请求都会基于此client实例
     */
    fun setClient(config: (OkHttpClient.Builder.() -> Unit)? = null) {
        if (config != null) {
            baseClient = baseClient.newBuilder().apply(config).apply { CallManager.bind(this) }.build()
        }
    }

    fun setCacheDir(cacheDir: File) {
        ResponseCache.init(cacheDir)
    }

    fun setLogEnabled(enable: Boolean) {
        OkhttpLogger.enabled = enable
    }

    fun newClient(config: (OkHttpClient.Builder.() -> Unit)? = null): OkHttpClient {
        val builder = baseClient.newBuilder().apply { CallManager.bind(this) }
        if (config != null) {
            builder.apply(config)
        }
        return builder.build()
    }

    /**
     * 添加自定义的转换器，用于解析响应
     *
     * 如果存在相同转换类型 [ResponseConverter.supportType] 的转换器，则替换已有的转换器
     */
    fun addConverter(converter: ResponseConverter<*>) {
        // 移除相同类型的转换器
        converters.removeAll { it.supportType() == converter.supportType() }
        converters.add(converter)
    }

    private fun findConverter(type: KClass<*>): ResponseConverter<*>? {
        return converters.find { it.supportType() == type }
    }

    /**
     * 执行同步请求
     * 此函数通过高阶函数参数接收请求配置，允许在执行前动态指定请求的各种参数
     * 主要用于在保持代码灵活和可读性的同时，执行一次性配置的请求
     *
     * @param config 请求的配置块，用于在执行请求前设置请求的各种参数
     * @return 返回请求的执行结果，类型为String
     * @throws Exception 如果请求执行过程中发生异常，则抛出此异常
     */
    @Throws(Exception::class)
    inline fun <reified R> execute(config: UrlRequest<R>.() -> Unit): R {
        return execute(UrlRequest<R>().apply(config), R::class)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R> convertResponse(request: UrlRequest<R>, type: KClass<*>, response: Response): R {
        request.onResponse?.invoke(response)
        val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
        if (converter == null) {
            throw Exception("Converter not found for $type")
        } else {
            val converted = converter.convert(request, response)
            request.onSuccess?.invoke(converted)
            return converted
        }
    }

    /**
     * 执行同步请求
     * @param request BoxedRequest类型的请求对象，包含了所有与请求相关的信息，
     *                如请求头、请求体、请求类型等
     * @return 返回一个字符串类型的响应结果，通常为JSON、XML或其他自定义的格式，
     *         具体格式取决于系统设计和业务需求
     */
    @Throws(Exception::class)
    @Suppress("UNCHECKED_CAST")
    fun <R> execute(request: UrlRequest<R>, type: KClass<*>): R {
        val cacheKey = request.standAloneCacheKay ?: request.request.cacheKey
        return when (request.cacheMode) {
            CacheMode.ONLY_CACHE -> {
                val cache = ResponseCache.get(cacheKey, request.request, request.cacheValidDuration)
                if (cache == null) {
                    throw IOException("Cache not found!")
                }

                convertResponse(request, type, cache)
            }

            CacheMode.ONLY_NETWORK -> {
                val response = try {
                    executeResponse(request.client, cacheKey, request.request, request.cacheValidDuration)
                } catch (e: Exception) {
                    throw e
                }

                convertResponse(request, type, response)
            }

            CacheMode.NETWORK_ELSE_CACHE -> {
                val response = try {
                    executeResponse(
                        request.client, cacheKey, request.request, request.cacheValidDuration
                    )
                } catch (e: Exception) {
                    ResponseCache.get(cacheKey, request.request, request.cacheValidDuration)
                        ?: throw e
                }

                convertResponse(request, type, response)
            }

            CacheMode.CACHE_ELSE_NETWORK -> {
                val response = ResponseCache.get(cacheKey, request.request, request.cacheValidDuration) ?: try {
                    executeResponse(
                        request.client, cacheKey, request.request, request.cacheValidDuration
                    )
                } catch (e: Exception) {
                    throw e
                }

                convertResponse(request, type, response)
            }
        }
    }

    @Throws(IOException::class)
    private fun executeResponse(
        client: OkHttpClient, cacheKey: String? = null, request: Request, cacheValidDuration: Long
    ): Response {
        // 发送请求并获取响应对象
        client.newCall(request).execute().use { response ->
            // 如果响应指示需要重定向
            if (response.isRedirect) {
                // 获取重定向的位置信息，如果不存在则抛出异常
                val location = response.header("Location")
                if (location == null) {
                    throw IOException("Response is redirected but location not found!")
                }
                // 使用新的URL重新构建请求并执行
                return executeResponse(client, cacheKey, request.newBuilder().url(location).build(), cacheValidDuration)
            }

            // 如果请求成功
            if (response.isSuccessful) {
                // 返回响应体的内容，使用指定的字符集进行解析
                return ResponseCache.put(cacheKey, request, response, cacheValidDuration)
            } else {
                // 如果请求失败，根据响应码抛出异常
                throw IOException("Request failed with code ${response.code}")
            }
        }
    }

    /**
     * 将打包的请求加入队列
     *
     * 此方法将一个BoxedRequest对象加入处理队列，BoxedRequest包含了执行请求所需的所有信息
     * 包括具体的请求、字符集、错误处理回调和成功处理回调
     *
     * @param request BoxedRequest对象，包含了所有执行请求所需的信息
     */
    inline fun <reified R> enqueue(request: UrlRequest<R>) {
        enqueue(request, R::class)
    }

    /**
     * 将一个配置化的请求加入队列
     *
     * @param config BoxedRequest的Lambda表达式，用于在调用点内配置请求
     *
     * 此函数的作用是提供一种链式调用的方式，使得请求的配置更加清晰和简洁
     * 它首先创建一个BoxedRequest实例，然后应用传入的配置Lambda来配置这个请求
     * 最后，它将这个配置好的请求以及其成功和失败的回调加入到处理队列中
     *
     * 为什么这么做：
     * 这种设计允许调用者以更加直观和便捷的方式配置和添加请求，避免了繁琐的构造器或者多个参数的方法调用
     *
     */
    inline fun <reified R> enqueue(config: UrlRequest<R>.() -> Unit) {
        enqueue(UrlRequest<R>().apply(config), R::class)
    }

    @Suppress("UNCHECKED_CAST")
    fun <R> enqueue(request: UrlRequest<R>, type: KClass<*>) {
        thread {
            val cacheKey = request.standAloneCacheKay ?: request.request.cacheKey
            when (request.cacheMode) {
                CacheMode.ONLY_CACHE -> {
                    val cache = ResponseCache.get(cacheKey, request.request, request.cacheValidDuration)
                    if (cache == null) {
                        request.onError?.invoke(IOException("Cache not found!"))
                    } else {
                        request.onResponse?.invoke(cache)
                        val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
                        if (converter == null) {
                            request.onError?.invoke(IOException("Converter not found for $type"))
                        } else {
                            try {
                                request.onSuccess?.invoke(converter.convert(request, cache))
                            } catch (e: Throwable) {
                                request.onError?.invoke(e)
                            }
                        }
                    }
                }

                CacheMode.ONLY_NETWORK -> {
                    enqueueResponse(request.client, cacheKey, request.request, request.cacheValidDuration, onError = {
                        request.onError?.invoke(it)
                    }, onSuccess = { response ->
                        request.onResponse?.invoke(response)
                        val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
                        if (converter == null) {
                            request.onError?.invoke(IOException("Converter not found for $type"))
                        } else {
                            try {
                                request.onSuccess?.invoke(converter.convert(request, response))
                            } catch (e: Throwable) {
                                request.onError?.invoke(e)
                            }
                        }
                    })
                }

                CacheMode.NETWORK_ELSE_CACHE -> {
                    enqueueResponse(
                        request.client,
                        cacheKey,
                        request.request,
                        request.cacheValidDuration,
                        onError = { e ->
                            val cache = ResponseCache.get(cacheKey, request.request, request.cacheValidDuration)
                            if (cache == null) {
                                request.onError?.invoke(IOException(buildString {
                                    append(e.message)
                                    append(" and cache not found!")
                                }))
                            } else {
                                request.onResponse?.invoke(cache)
                                val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
                                if (converter == null) {
                                    request.onError?.invoke(IOException("Converter not found for $type"))
                                } else {
                                    try {
                                        request.onSuccess?.invoke(converter.convert(request, cache))
                                    } catch (e: Throwable) {
                                        request.onError?.invoke(e)
                                    }
                                }
                            }
                        },
                        onSuccess = { response ->
                            request.onResponse?.invoke(response)
                            val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
                            if (converter == null) {
                                request.onError?.invoke(IOException("Converter not found for $type"))
                            } else {
                                try {
                                    request.onSuccess?.invoke(converter.convert(request, response))
                                } catch (e: Throwable) {
                                    request.onError?.invoke(e)
                                }
                            }
                        })
                }

                CacheMode.CACHE_ELSE_NETWORK -> {
                    val cache = ResponseCache.get(cacheKey, request.request, request.cacheValidDuration)
                    if (cache != null) {
                        request.onResponse?.invoke(cache)
                        val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
                        if (converter == null) {
                            request.onError?.invoke(IOException("Converter not found for $type"))
                        } else {
                            try {
                                request.onSuccess?.invoke(converter.convert(request, cache))
                            } catch (e: Throwable) {
                                request.onError?.invoke(e)
                            }
                        }
                    } else {
                        enqueueResponse(
                            request.client,
                            cacheKey,
                            request.request,
                            request.cacheValidDuration,
                            onError = {
                                request.onError?.invoke(it)
                            },
                            onSuccess = { response ->
                                request.onResponse?.invoke(response)
                                val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
                                if (converter == null) {
                                    request.onError?.invoke(IOException("Converter not found for $type"))
                                } else {
                                    try {
                                        request.onSuccess?.invoke(converter.convert(request, response))
                                    } catch (e: Throwable) {
                                        request.onError?.invoke(e)
                                    }
                                }
                            })
                    }
                }
            }
        }
    }

    private fun enqueueResponse(
        client: OkHttpClient,
        cacheKey: String?,
        request: Request,
        cacheValidDuration: Long,
        onError: ((e: Exception) -> Unit)? = null,
        onSuccess: ((response: Response) -> Unit)? = null
    ) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError?.invoke(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isRedirect) {
                    val location = response.header("Location")
                    if (location.isNullOrEmpty()) {
                        onError?.invoke(IOException("Response is redirected but location not found!"))
                    } else {
                        enqueueResponse(
                            client,
                            cacheKey,
                            request.newBuilder().url(location).build(),
                            cacheValidDuration,
                            onError,
                            onSuccess
                        )
                    }
                } else {
                    if (response.isSuccessful) {
                        onSuccess?.invoke(ResponseCache.put(cacheKey, request, response, cacheValidDuration))
                    } else {
                        onError?.invoke(IOException("Request failed with code ${response.code}"))
                    }
                }
            }
        })
    }

    /**
     * 同步下载文件
     */
    @Throws(Exception::class)
    fun download(config: DownloadRequest.() -> Unit): File {
        val request = DownloadRequest().apply(config)
        if (request.downloadDir == null) throw IOException("Download dir is null!")
        return download(request).also {
            request.onSuccess?.invoke(it)
        }
    }

    @Throws(Exception::class)
    fun download(request: DownloadRequest): File {
        var newRequest = request.request
        if (request.enableResumeDownload && request.downloadFileName != null) {
            val file = File(request.downloadDir, request.downloadFileName!!)
            val configFile = File(request.downloadDir, ".${request.downloadFileName}.cfg")
            if (request.overwrite && file.exists()) {
                file.delete()
                configFile.delete()
            }
            if (file.exists()) {
                //"bytes=" + startPos + "-" + endPos
                //未指定endPos ，则下载剩余部分
                newRequest = newRequest.newBuilder().header("Range", "bytes=${file.length()}-").build()
            }
        }

        val call = request.client.newCall(newRequest)

        fun verifyFile(file: File) {
            if (request.md5.isNotEmpty()) {
                if (!file.verifyMD5(call, request.md5, request.onVerifyFile)) {
                    throw FileVerificationException("File MD5 verification failed!")
                }
            }

            if (request.sha1.isNotEmpty()) {
                if (!file.verifySHA1(call, request.sha1, request.onVerifyFile)) {
                    throw FileVerificationException("File SHA-1 verification failed!")
                }
            }

            if (request.sha256.isNotEmpty()) {
                if (!file.verifyShA256(call, request.sha256, request.onVerifyFile)) {
                    throw FileVerificationException("File SHA-256 verification failed!")
                }
            }
        }

        call.execute().use { response ->
            if (response.isRedirect) {
                val location = response.header("Location")
                if (location.isNullOrEmpty()) {
                    throw IOException("Response is redirected but location not found!")
                }

                return download(request.apply { url(location) })
            }

            if (!response.isSuccessful && response.code != 416) {
                throw IOException("Response is not successful: ${response.code}")
            }
            request.onResponse?.invoke(response)
            try {
                val file = File(request.downloadDir, request.downloadFileName ?: response.fileName())
                val contentLength = response.body?.contentLength() ?: -1L
                val configFile = File(request.downloadDir, ".${file.name}.cfg")
                when (response.code) {
                    416 -> { //416一般为请求的文件大小范围超出服务器文件大小范围
                        if (file.exists() && file.length() > 0L) {
                            verifyFile(file)
                            configFile.delete()
                            return file
                        } else {
                            throw IOException("Request content range error, code : ${response.code}")
                        }
                    }

                    206 -> {
                        //Content-Range: bytes 18333696-6114656255/6114656256
                        val rangeInfo = response.headers["Content-Range"]
                        if (rangeInfo == null) {
                            throw IOException("Content-Range is null!")
                        }

                        val range = rangeInfo.substringAfter("bytes").substringBefore("/")
                        val startPos = range.substringBefore("-").trim().toLong()

                        //如果文件已存在，则读取配置文件中的TotalBytes
                        var fullBytes = -1L
                        val localFileBytes = file.length()
                        if (configFile.exists()) {
                            fullBytes = configFile.reader().use {
                                it.readText().substringAfter("ContentLength=").toLong()
                            }
                        }

                        response.body?.source()?.inputStream()?.use { input ->
                            RandomAccessFile(file, "rwd").use {
                                println("断点续传: startPos = $startPos")
                                it.seek(startPos)
                                input.copyTo(contentLength, it) { percent, bytesCopied, totalBytes ->
                                    if (fullBytes == -1L) {
                                        request.onProgress?.invoke(percent, bytesCopied, totalBytes)
                                    } else {
                                        val newBytesCopied = localFileBytes + bytesCopied
                                        val newPercent = newBytesCopied / fullBytes.toFloat() * 100
                                        request.onProgress?.invoke(newPercent, newBytesCopied, fullBytes)
                                    }
                                }
                            }
                        } ?: let {
                            throw IOException("Response body is null!")
                        }

                        verifyFile(file)
                        configFile.delete()
                        return file
                    }

                    else -> {
                        if (request.overwrite && file.exists()) {
                            file.delete()
                            configFile.delete()
                        }

                        if (file.exists() && file.length() == contentLength) {
                            verifyFile(file)
                            configFile.delete()
                            return file
                        }

                        configFile.outputStream().writer().use {
                            it.write("ContentLength=$contentLength")
                        }

                        return response.body?.source()?.inputStream()?.use { input ->
                            file.createNewFile()
                            file.outputStream().use { out ->
                                input.copyTo(contentLength, out, request.onProgress)
                            }

                            verifyFile(file)
                            configFile.delete()
                            file
                        } ?: let {
                            configFile.delete()
                            throw IOException("Response body is null!")
                        }
                    }
                }
            } catch (e: Exception) {
                if (call.isCanceled()) {
                    throw CallCanceledException("Call is canceled!")
                } else {
                    throw e
                }
            }
        }
    }

    /**
     * 异步下载文件
     */
    fun downloadAsync(config: DownloadRequest.() -> Unit) {
        val request = DownloadRequest().apply(config)
        if (request.downloadDir == null) {
            request.onError?.invoke(IOException("Download dir is null!"))
        } else {
            downloadAsync(request)
        }
    }

    /**
     * 异步下载文件
     */
    fun downloadAsync(request: DownloadRequest) {
        var newRequest = request.request
        if (request.enableResumeDownload && request.downloadFileName != null) {
            val file = File(request.downloadDir, request.downloadFileName!!)
            val configFile = File(request.downloadDir, ".${request.downloadFileName}.cfg")
            if (request.overwrite && file.exists()) {
                file.delete()
                configFile.delete()
            }
            if (file.exists()) {
                newRequest = newRequest.newBuilder().header("Range", "bytes=${file.length()}-").build()
            }
        }

        val call = request.client.newCall(newRequest)

        fun verifyFile(file: File) {
            if (request.md5.isNotEmpty()) {
                if (!file.verifyMD5(call, request.md5, request.onVerifyFile)) {
                    request.onError?.invoke(FileVerificationException("File MD5 verification failed!"))
                    return
                }
            }

            if (request.sha1.isNotEmpty()) {
                if (!file.verifySHA1(call, request.sha1, request.onVerifyFile)) {
                    request.onError?.invoke(FileVerificationException("File SHA-1 verification failed!"))
                    return
                }
            }

            if (request.sha256.isNotEmpty()) {
                if (!file.verifyShA256(call, request.sha256, request.onVerifyFile)) {
                    request.onError?.invoke(FileVerificationException("File SHA-256 verification failed!"))
                    return
                }
            }

            request.onSuccess?.invoke(file)
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                request.onError?.invoke(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isRedirect) {
                        val location = resp.header("Location")
                        if (location.isNullOrEmpty()) {
                            request.onError?.invoke(IOException("Response is redirected but location not found!"))
                        } else {
                            downloadAsync(
                                request.apply { url(location) })
                        }
                        return
                    }

                    if (!resp.isSuccessful && resp.code != 416) {
                        request.onError?.invoke(IOException("Response is not successful: ${resp.code}"))
                        return
                    }

                    request.onResponse?.invoke(response)
                    try {
                        val file = File(request.downloadDir, request.downloadFileName ?: resp.fileName())
                        val configFile = File(request.downloadDir, "${file.name}.cfg")
                        val contentLength = resp.body?.contentLength() ?: -1L
                        when (resp.code) {
                            416 -> {//416一般为请求的文件大小范围超出服务器文件大小范围
                                if (file.exists() && file.length() > 0L) {
                                    verifyFile(file)
                                    configFile.delete()
                                } else {
                                    request.onError?.invoke(IOException("Request content range error, code : ${resp.code}"))
                                }
                            }

                            206 -> {
                                //Content-Range: bytes 18333696-6114656255/6114656256
                                val rangeInfo = resp.headers["Content-Range"]
                                if (rangeInfo == null) {
                                    request.onError?.invoke(IOException("Content-Range is null!"))
                                } else {
                                    val range = rangeInfo.substringAfter("bytes").substringBefore("/")
                                    val startPos = range.substringBefore("-").trim().toLong()

                                    //如果文件已存在，则读取配置文件中的TotalBytes
                                    var fullBytes = -1L
                                    val localFileBytes = file.length()
                                    if (configFile.exists()) {
                                        try {
                                            fullBytes = configFile.reader().use {
                                                it.readText().substringAfter("ContentLength=").toLong()
                                            }
                                        } catch (e: Throwable) {
                                            e.printStackTrace()
                                        }
                                    }

                                    resp.body?.source()?.inputStream()?.use { input ->
                                        println("断点续传: startPos = $startPos")
                                        RandomAccessFile(file, "rwd").use {
                                            it.seek(startPos)
                                            input.copyTo(contentLength, it) { percent, bytesCopied, totalBytes ->
                                                if (fullBytes == -1L) {
                                                    request.onProgress?.invoke(percent, bytesCopied, totalBytes)
                                                } else {
                                                    val newBytesCopied = localFileBytes + bytesCopied
                                                    val newPercent = newBytesCopied / fullBytes.toFloat() * 100
                                                    request.onProgress?.invoke(newPercent, newBytesCopied, fullBytes)
                                                }
                                            }
                                        }
                                        verifyFile(file)
                                        configFile.delete()
                                    } ?: let {
                                        request.onError?.invoke(IOException("Response body is null!"))
                                    }
                                }
                            }

                            else -> {
                                if (request.overwrite && file.exists()) {
                                    file.delete()
                                    configFile.delete()
                                }
                                if (file.exists() && file.length() == contentLength) {
                                    verifyFile(file)
                                    configFile.delete()
                                } else {
                                    configFile.outputStream().writer().use {
                                        it.write("ContentLength=$contentLength")
                                    }

                                    resp.body?.source()?.inputStream()?.use { input ->
                                        file.createNewFile()
                                        file.outputStream().use { out ->
                                            input.copyTo(contentLength, out, request.onProgress)
                                        }
                                        verifyFile(file)
                                        configFile.delete()
                                    } ?: let {
                                        request.onError?.invoke(IOException("Response body is null!"))
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        if (!call.isCanceled()) {
                            //如果已取消请求，则不抛出异常
                            request.onError?.invoke(e)
                        } else {
                            println("Call isCanceled!")
                        }
                    }
                }
            }
        })
    }

    /**
     * 同步上传文件
     */
    @Throws(Exception::class)
    inline fun <reified R> upload(config: UploadRequest<R>.() -> Unit): R {
        return upload(UploadRequest<R>().apply(config), R::class)
    }

    /**
     * 同步上传文件
     */
    @Throws(Exception::class)
    @Suppress("UNCHECKED_CAST")
    fun <R> upload(request: UploadRequest<R>, type: KClass<*>): R {
        return uploadForResponse(request.client, request.request).use { //这里使用 use 后，会自动关闭流
            request.onResponse?.invoke(it)

            val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
            if (converter == null) {
                throw Exception("Converter not found for $type")
            }
            converter.convert(request, it)
        }.also {
            request.onSuccess?.invoke(it)
        }
    }

    @Throws(IOException::class)
    private fun uploadForResponse(client: OkHttpClient, request: Request): Response {
        // 发送请求并获取响应对象
        client.newCall(request).execute().let { response -> //这里使用 let 后，防止自动关闭流导致Closed
            // 如果响应指示需要重定向
            if (response.isRedirect) {
                // 获取重定向的位置信息，如果不存在则抛出异常
                val location =
                    response.header("Location") ?: throw IOException("Response is redirected but location not found!")
                // 使用新的URL重新构建请求并执行
                return uploadForResponse(client, request.newBuilder().url(location).build())
            }
            // 如果请求成功
            if (response.isSuccessful) {
                // 返回响应体的内容，使用指定的字符集进行解析
                return response
            } else {
                // 如果请求失败，根据响应码抛出异常
                throw IOException("Request failed with code ${response.code}")
            }
        }
    }

    /**
     * 异步上传文件
     */
    inline fun <reified R> uploadAsync(request: UploadRequest<R>.() -> Unit) {
        uploadAsync(UploadRequest<R>().apply(request), R::class)
    }

    /**
     * 异步上传文件
     */
    inline fun <reified R> uploadAsync(request: UploadRequest<R>) {
        uploadAsync(request, R::class)
    }

    @Suppress("UNCHECKED_CAST")
    fun <R> uploadAsync(request: UploadRequest<R>, type: KClass<*>) {
        uploadAsyncForResponse(request.client, request.request, onError = {
            request.onError?.invoke(it)
        }, onSuccess = { response ->
            request.onResponse?.invoke(response)
            val converter = request.converter ?: findConverter(type) as? ResponseConverter<R>
            if (converter == null) {
                request.onError?.invoke(IOException("Converter not found for $type"))
            } else {
                try {
                    request.onSuccess?.invoke(converter.convert(request, response))
                } catch (e: Throwable) {
                    request.onError?.invoke(e)
                }
            }
        })
    }

    private fun uploadAsyncForResponse(
        client: OkHttpClient, request: Request, onError: ((Exception) -> Unit)?, onSuccess: ((Response) -> Unit)?
    ) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError?.invoke(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isRedirect) {
                    val location = response.header("Location")
                    if (location == null) {
                        onError?.invoke(IOException("Response is redirected but location not found!"))
                    } else {
                        uploadAsyncForResponse(client, request.newBuilder().url(location).build(), onError, onSuccess)
                    }
                } else {
                    if (response.isSuccessful) {
                        onSuccess?.invoke(response)
                    } else {
                        onError?.invoke(IOException("Request failed with code ${response.code}"))
                    }
                }
            }
        })
    }

    fun cancel(call: Call) {
        CallManager.cancel(call)
    }

    fun cancelByTag(tag: Any) {
        CallManager.cancelByTag(tag)
    }

    fun cancelAll() {
        CallManager.cancelAll()
    }
}