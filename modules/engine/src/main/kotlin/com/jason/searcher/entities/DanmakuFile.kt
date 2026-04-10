package com.jason.searcher.entities

import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.Serializable

open class DanmakuFile(open val file: File) : Serializable, Closeable {
    @Transient
    private val writer: BufferedWriter = file.bufferedWriter()

    fun appendDanmaku(danmaku: DanmakuEntity): DanmakuFile {
        writer.appendLine(danmaku.toJSONObject().toString())
        return this
    }

    fun getList(): List<DanmakuEntity> {
        return buildList {
            file.bufferedReader().lines().forEach { line ->
                try {
                    add(DanmakuEntity.convertFromJSON(line))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun flush() {
        writer.flush()
    }

    override fun close() {
        writer.close()
    }
}