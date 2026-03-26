package com.jason.searcher.entities

data class PlayUrlEntity(
    val url: String,
    val audioUrl: String = "",
    val headers: HashMap<String, String> = hashMapOf(),
    val danmakuFile: DanmakuFile? = null,
    val externalSubtitles: List<VideoSubtitleEntity> = emptyList()
)
