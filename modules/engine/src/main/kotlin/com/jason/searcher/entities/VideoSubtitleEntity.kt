package com.jason.searcher.entities

/**
 * @param title 字幕名
 * @param url 字幕文件地址
 * @param language 语言 如 chi en ja
 * @param extension 字幕文件后缀 如 srt ass
 */
data class VideoSubtitleEntity(val title: String, val url: String, val language: String, val extension: String)
