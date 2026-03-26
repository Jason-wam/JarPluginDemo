package com.jason.searcher.entities

import com.google.gson.stream.JsonReader
import org.json.JSONArray
import org.json.JSONObject
import com.jason.searcher.utils.putIfNotEmpty
import okhttp3.Headers
import java.io.Serializable

class VideoDetailEntity : Serializable {
    //@id 必须为唯一Id且不可变化
    var id: String = ""
    var url: String = ""
    var title: String = ""
    var subtitle: String = ""
    var cover: String = ""
    var coverRatio: VideoCoverRatio = VideoCoverRatio.Auto
    var description: String = ""
    var browserUrl: String = ""

    var channels: ArrayList<VideoDetailChannelEntity> = arrayListOf()
    var sourceId: String = ""
    var sourceName: String = ""

    companion object {
        fun createFromJSON(json: String): VideoDetailEntity {
            return createFromJSON(JsonReader(json.reader()))
        }

        fun createFromJSON(baseURL: String, json: String): VideoDetailEntity {
            return createFromJSON(JsonReader(json.reader())).apply {
                url = baseURL
            }
        }

        fun createFromJSON(reader: JsonReader): VideoDetailEntity {
            val videoDetail = VideoDetailEntity()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> videoDetail.id = reader.nextString()
                    "title" -> videoDetail.title = reader.nextString()
                    "cover" -> videoDetail.cover = reader.nextString()
                    "coverRatio" -> videoDetail.coverRatio = VideoCoverRatio.Fixed(reader.nextDouble())
                    "subtitle" -> videoDetail.subtitle = reader.nextString()
                    "description" -> videoDetail.description = reader.nextString()
                    "browserUrl" -> videoDetail.browserUrl = reader.nextString()
                    "channels" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            videoDetail.channels.add(readSource(reader))
                        }
                        reader.endArray()
                    }

                    "sourceId" -> videoDetail.sourceId = reader.nextString()
                    "sourceName" -> videoDetail.sourceName = reader.nextString()
                }
            }
            reader.endObject()
            return videoDetail
        }

        private fun readSource(reader: JsonReader): VideoDetailChannelEntity {
            val source = VideoDetailChannelEntity()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> source.id = reader.nextString()
                    "name" -> source.title = reader.nextString()
                    "weight" -> source.weight = reader.nextInt()
                    "videoDataList" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            source.videoDataList.add(readVideoDataList(reader))
                        }
                        reader.endArray()
                    }
                }
            }
            reader.endObject()
            return source
        }

        private fun readVideoDataList(reader: JsonReader): VideoDetailDataEntity {
            val videoData = VideoDetailDataEntity()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> videoData.id = reader.nextString()
                    "url" -> videoData.videoUrl = reader.nextString()
                    "name" -> videoData.title = reader.nextString()
                    "isLive" -> videoData.isLive = reader.nextBoolean()
                    "canDownload" -> videoData.canDownload = reader.nextBoolean()
                    "regexVideoUrlPattern" -> videoData.regexVideoUrlPattern = reader.nextString()

                    "headers" -> {
                        reader.beginObject()
                        videoData.headers = hashMapOf<String, String>().apply {
                            while (reader.hasNext()) {
                                put(reader.nextName(), reader.nextString())
                            }
                        }
                    }


                    "videoType" -> {
                        videoData.videoType = reader.nextString().let {
                            VideoType.valueOf(it)
                        }
                    }

                    "sniffPatterns" -> {
                        reader.beginArray()
                        videoData.sniffPatterns = buildList {
                            while (reader.hasNext()) {
                                add(reader.nextString())
                            }
                        }
                        reader.endArray()
                    }

                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return videoData
        }

        fun createFromJSON(obj: JSONObject): VideoDetailEntity {
            return VideoDetailEntity().apply {
                id = obj.optString("id")
                title = obj.optString("title")
                subtitle = obj.optString("subtitle")
                cover = obj.optString("cover")
                coverRatio = VideoCoverRatio.Fixed(obj.optDouble("coverRatio"))
                description = obj.optString("description")
                browserUrl = obj.optString("browserUrl")

                obj.optJSONArray("channels")?.let { array ->
                    channels.addAll(buildList {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            add(VideoDetailChannelEntity().apply {
                                id = obj.optString("id")
                                title = obj.optString("name")
                                weight = obj.optInt("weight")
                                obj.optJSONArray("videoDataList")?.let {
                                    videoDataList.addAll(buildList {
                                        for (i in 0 until it.length()) {
                                            add(
                                                VideoDetailDataEntity.createFromJSON(
                                                    it.getJSONObject(
                                                        i
                                                    )
                                                )
                                            )
                                        }
                                    })
                                }
                            })
                        }
                    })
                }

                sourceId = obj.optString("sourceId")
                sourceName = obj.optString("sourceName")
            }
        }
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            putIfNotEmpty("subtitle", subtitle)
            put("cover", cover)
            put("coverRatio", coverRatio.value)
            putIfNotEmpty("description", description)
            put("browserUrl", browserUrl)

            put("channels", JSONArray().apply {
                channels.forEach {
                    put(it.toJSONObject())
                }
            })
            put("sourceId", sourceId)
            put("sourceName", sourceName)
        }
    }
}