package com.jason.searcher.entities

data class DownloadUrlEntity(
    val url: String,
    val headers: HashMap<String, String> = hashMapOf(),
    val danmakuFile: DanmakuFile? = null
)
