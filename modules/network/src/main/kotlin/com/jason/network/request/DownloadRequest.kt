package com.jason.network.request

import com.jason.network.OkHttpClientUtil
import java.io.File

@Suppress("unused")
class DownloadRequest : BaseRequest<File>() {
    internal var downloadDir: File? = null
    internal var downloadFileName: String? = null
    internal var overwrite: Boolean = false

    internal var onProgress: ((percent: Float, downloadBytes: Long, totalBytes: Long) -> Unit)? = null
    internal var enableResumeDownload: Boolean = false
    internal var md5: String = ""
    internal var sha1: String = ""
    internal var sha256: String = ""
    internal var onVerifyFile: ((percent: Float, downloadBytes: Long, totalBytes: Long) -> Unit)? = null

    /**
     * 下载大文件时使用长连接覆盖原始的短连接
     */
    override var client = OkHttpClientUtil.longClient

    fun setDownloadDir(dir: File) {
        this.downloadDir = dir
    }

    fun setDownloadFileName(filename: String) {
        this.downloadFileName = filename
    }

    /**
     * 设置是否覆盖已存在的文件
     */
    fun setOverwrite(overwrite: Boolean) {
        this.overwrite = overwrite
    }

    /**
     * 开启断点续传, 默认不开启且必须指定文件名方可生效
     * 优先级低于 [overwrite],如果设置了 [overwrite] 为 true,则不会生效
     */
    fun setEnableResumeDownload(enableResumeDownload: Boolean) {
        this.enableResumeDownload = enableResumeDownload
    }

    /**
     * 设置校验文件 MD5 值
     *
     * MD5、SHA-1、SHA-256 三种校验方式依次执行，校验顺序为 MD5 -> SHA-1 -> SHA-256
     */
    fun setMD5(md5: String) {
        this.md5 = md5
    }

    /**
     * 设置校验文件 SHA-1 值
     *
     * MD5、SHA-1、SHA-256 三种校验方式依次执行，校验顺序为 MD5 -> SHA-1 -> SHA-256
     */
    fun setSHA1(sha1: String) {
        this.sha1 = sha1
    }

    /**
     * 设置校验文件 SHA-256 值
     *
     * MD5、SHA-1、SHA-256 三种校验方式依次执行，校验顺序为 MD5 -> SHA-1 -> SHA-256
     */
    fun setSHA256(sha256: String) {
        this.sha256 = sha256
    }

    /**
     * 文件校验进度监听
     *
     * 如果文件校验不通过则会抛出异常，异步下载则会回调 [onError]
     */
    fun onVerifyFile(onVerifyFile: ((percent: Float, verifiedBytes: Long, totalBytes: Long) -> Unit)) {
        this.onVerifyFile = onVerifyFile
    }

    fun onProgress(onProgress: ((percent: Float, downloadBytes: Long, totalBytes: Long) -> Unit)) {
        this.onProgress = onProgress
    }
}