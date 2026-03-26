package com.jason.searcher.entities

import org.json.JSONObject

/**
 * @param  text 弹幕内容
 * @param  time 弹幕时间 单位毫秒
 * @param  type 弹幕类型
 * @param  color 弹幕颜色 16进制
 */
data class DanmakuEntity(
    val text: String,
    val time: Long,
    val type: Int = DanmakuType.TYPE_SCROLL_RL,
    val color: String = "#FFFFFF"
) {
    companion object {
        fun convertFromJSON(json: String): DanmakuEntity {
            val obj = JSONObject(json)
            return DanmakuEntity(
                obj.getString("text"),
                obj.getLong("time"),
                obj.getInt("type"),
                obj.getString("color")
            )
        }
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("text", text)
            put("time", time)
            put("type", type)
            put("color", color)
        }
    }
}

object DanmakuType {
    /**
     * 滚动弹幕
     */
    const val TYPE_SCROLL_RL: Int = 1

    /**
     * 固定顶部弹幕
     */
    const val TYPE_FIX_TOP: Int = 5

    /**
     * 固定底部弹幕
     */
    const val TYPE_FIX_BOTTOM: Int = 4
}