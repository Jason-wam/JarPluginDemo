package com.jason.searcher.entities

import org.json.JSONArray
import org.json.JSONObject
import com.jason.searcher.utils.putIfNotEmpty
import okhttp3.Headers
import java.io.Serializable

class VideoDetailDataEntity : Cloneable, Serializable {
    var id: String = ""
    var title: String = ""
    var subtitle: String = ""
    var cover: String = ""
    var coverRatio: VideoCoverRatio = VideoCoverRatio.Auto

    var videoUrl: String = ""
    var videoType: VideoType = VideoType.Direct

    var isLive: Boolean = false
    var canDownload: Boolean = false

    var regexVideoUrlPattern: String = ""
    var headers: HashMap<String, String> = hashMapOf()
    var sniffPatterns: List<String> = emptyList()

    companion object {
        fun createFromJSON(json: String): VideoDetailDataEntity {
            return createFromJSON(JSONObject(json))
        }

        fun createFromJSON(obj: JSONObject): VideoDetailDataEntity {
            return VideoDetailDataEntity().apply {
                id = obj.optString("id")
                title = obj.optString("name")
                subtitle = obj.optString("subtitle")
                cover = obj.optString("cover")
                coverRatio = VideoCoverRatio.Fixed(obj.optDouble("coverRatio", 0.0))
                videoUrl = obj.optString("videoUrl")

                isLive = obj.optBoolean("isLive")
                videoType = obj.optString("videoType", "Direct").let {
                    VideoType.valueOf(it)
                }
                obj.optJSONObject("headers")?.let {
                    headers = HashMap<String, String>().apply {
                        it.keys().forEach { key ->
                            put(key, it.getString(key))
                        }
                    }
                }

                canDownload = obj.optBoolean("canDownload")
                regexVideoUrlPattern = obj.optString("regexVideoUrlPattern")
                obj.optJSONArray("sniffPatterns")?.let {
                    sniffPatterns = buildList {
                        for (i in 0 until it.length()) {
                            add(it.getString(i))
                        }
                    }
                }
            }
        }
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("subtitle", subtitle)
            putIfNotEmpty("cover", cover)
            if (cover.isNotEmpty()) {
                put("coverRatio", coverRatio.value)
            }

            put("videoUrl", videoUrl)
            put("videoType", videoType.name)

            if (headers.isNotEmpty()) {
                put("headers", JSONObject().apply {
                    headers.forEach { (key, value) ->
                        put(key, value)
                    }
                })
            }

            put("isLive", isLive)
            put("canDownload", canDownload)

            putIfNotEmpty("regexVideoUrlPattern", regexVideoUrlPattern)
            if (sniffPatterns.isNotEmpty()) {
                put("sniffPatterns", JSONArray().apply {
                    for (rule in sniffPatterns) {
                        put(rule)
                    }
                })
            }
        }
    }

    public override fun clone(): VideoDetailDataEntity {
        return VideoDetailDataEntity().apply {
            id = this@VideoDetailDataEntity.id
            title = this@VideoDetailDataEntity.title
            subtitle = this@VideoDetailDataEntity.subtitle
            cover = this@VideoDetailDataEntity.cover
            coverRatio = this@VideoDetailDataEntity.coverRatio
            videoUrl = this@VideoDetailDataEntity.videoUrl
            videoType = this@VideoDetailDataEntity.videoType

            headers = this@VideoDetailDataEntity.headers

            isLive = this@VideoDetailDataEntity.isLive
            canDownload = this@VideoDetailDataEntity.canDownload
            regexVideoUrlPattern = this@VideoDetailDataEntity.regexVideoUrlPattern
            sniffPatterns = this@VideoDetailDataEntity.sniffPatterns
        }
    }
}