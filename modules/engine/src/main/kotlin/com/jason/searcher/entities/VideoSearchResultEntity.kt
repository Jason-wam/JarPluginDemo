package com.jason.searcher.entities

import org.json.JSONObject
import java.io.Serializable

class VideoSearchResultEntity : Serializable {
    var title: String = ""
    var subtitle: String = ""
    var updateInfo: String = ""

    var score: String = ""
    var duration: String = ""

    var cover: String = ""
    var coverRatio: VideoCoverRatio = VideoCoverRatio.Auto
    var detailPageUrl: String = ""
    var detailPageUrlType: DetailPageUrlType = DetailPageUrlType.DetailPage


    var sourceId: String = ""
    var sourceName: String = ""

    enum class DetailPageUrlType {
        /**
         * 视频直链
         */
        Video,
        /**
         * 详情页面
         */
        DetailPage
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("subtitle", subtitle)
            put("updateInfo", updateInfo)
            put("score", score)
            put("duration", duration)
            put("cover", cover)
            put("coverRatio", coverRatio.value)
            put("detailPageUrl", detailPageUrl)
            put("detailPageUrlType", detailPageUrlType.name)

            put("sourceId", sourceId)
            put("sourceName", sourceName)
        }
    }
}