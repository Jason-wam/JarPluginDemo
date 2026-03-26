package com.jason.searcher.entities

import org.json.JSONObject
import com.jason.searcher.utils.putIfNotEmpty
import java.io.Serializable


fun MutableList<VideoClassEntity>.append(title: String, url: String) {
    add(VideoClassEntity.build(title, url))
}

class VideoClassEntity : Serializable {
    var url: String = ""
    var title: String = ""

    /**
     * 部分网址第一页可能忽略page参数
     * 可单独设定
     */
    var firstPageUrl: String = ""

    /**
     * 0 表示 GET 请求
     *
     * 1 表示 POST 请求
     */
    var method: Int = Method.GET

    /**
     * POST 请求数据
     */
    var postData: String = ""

    /**
     * POST 请求数据类型 默认 application/x-www-form-urlencoded
     */
    var postDataType: String = "application/x-www-form-urlencoded"

    var extraHeaders = linkedMapOf<String, String>()

    /**
     * 页码偏移量 默认为0
     *
     * 软件内部默认开始页码为 1
     *
     * 部分网页开始页码可能从0页开始，那么偏移量则可以设置为 -1
     *
     * 则软件内的真实页码为 page + pageOffset
     */
    var pageOffset: Int = 0

    var weight: Int = 0

    object Method {
        const val GET = 0
        const val POST = 1
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("url", url)
            put("title", title)
            putIfNotEmpty("firstPageUrl", firstPageUrl)
            put("weight", weight)
            if (pageOffset != 0) {
                put("pageOffset", pageOffset)
            }

            if (method == Method.POST) {
                put("method", method)
                put("postData", postData)
                put("postDataType", postDataType)
            }

            if (extraHeaders.isNotEmpty()) {
                put("extraHeaders", JSONObject().apply {
                    extraHeaders.forEach {
                        put(it.key, it.value)
                    }
                })
            }
        }
    }

    companion object {
        fun convertFromJSON(json: String) = convertFromJSON(JSONObject(json))

        fun convertFromJSON(obj: JSONObject): VideoClassEntity {
            return VideoClassEntity().apply {
                url = obj.optString("url")
                title = obj.optString("title")
                firstPageUrl = obj.optString("firstPageUrl")
                weight = obj.optInt("weight")
                postData = obj.optString("postData")
                postDataType = obj.optString("postDataType")
                pageOffset = obj.optInt("pageOffset")
                method = obj.optInt("method")
                obj.optJSONObject("extraHeaders")?.let {
                    it.keys().forEach { key ->
                        extraHeaders[key] = it.optString(key)
                    }
                }
            }
        }

        fun build(title: String, url: String): VideoClassEntity {
            return VideoClassEntity().apply {
                this.title = title
                this.url = url
            }
        }
    }
}