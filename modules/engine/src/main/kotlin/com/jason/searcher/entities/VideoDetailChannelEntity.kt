package com.jason.searcher.entities

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

class VideoDetailChannelEntity : Serializable {
    //@id 必须为唯一Id且不可变化
    var id: String = ""
    var title: String = ""
    var weight: Int = 0
    var videoDataList: ArrayList<VideoDetailDataEntity> = arrayListOf()

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            //@id 必须为唯一Id且不可变化
            put("id", id)
            put("title", title)
            put("weight", weight)
            put("videoDataList", JSONArray().apply {
                videoDataList.forEach {
                    put(it.toJSONObject())
                }
            })
        }
    }
}