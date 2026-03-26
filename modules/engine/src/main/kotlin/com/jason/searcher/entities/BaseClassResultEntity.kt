package com.jason.searcher.entities

import java.io.Serializable

open class BaseClassResultEntity(val hasMore: Boolean, val value: List<VideoClassResultEntity>): Serializable {
    companion object {
        val EMPTY = BaseClassResultEntity(false, emptyList())
    }
}