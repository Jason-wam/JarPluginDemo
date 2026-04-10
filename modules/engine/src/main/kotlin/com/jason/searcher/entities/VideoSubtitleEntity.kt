package com.jason.searcher.entities

import org.json.JSONObject
import java.io.Serializable

/**
 * @param title 字幕名
 * @param url 字幕文件地址
 * @param language 语言 如 chi en ja
 * @param extension 字幕文件后缀 如 srt ass
 */
data class VideoSubtitleEntity(
    val title: String, val url: String, val language: String, val extension: String
): Serializable{
    companion object {
        fun createFromJSON(json: String): VideoSubtitleEntity {
            val obj = JSONObject(json)
            return VideoSubtitleEntity(
                obj.getString("title"),
                obj.getString("url"),
                obj.getString("language"),
                obj.getString("extension")
            )
        }
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("url", url)
            put("language", language)
            put("extension", extension)
        }
    }
}
