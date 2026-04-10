package com.jason.searcher.entities

import org.json.JSONObject
import java.io.Serializable

class VideoClassResultEntity : Serializable {
    var title: String = ""
    var subtitle: String = ""

    var cover: String = ""
    var coverRatio: VideoCoverRatio = VideoCoverRatio.Auto

    var score: String = ""
    var duration: String = ""
    var detailPageUrl: String = ""
    var detailPageUrlType: DetailPageUrlType = DetailPageUrlType.DetailPage

    var sourceId: String = ""
    var sourceName: String = ""

    enum class DetailPageUrlType {
        Video, DetailPage
    }

    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("title", title)
        put("subtitle", subtitle)

        put("cover", cover)
        put("coverRatio", coverRatio.value)

        put("score", score)
        put("duration", duration)
        put("detailPageUrl", detailPageUrl)
        put("detailPageUrlType", detailPageUrlType.name)

        put("sourceId", sourceId)
        put("sourceName", sourceName)
    }
}