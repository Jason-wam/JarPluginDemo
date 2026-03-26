package com.jason.searcher.entities

import java.io.Serializable

open class BaseSearchResultEntity(val hasMore: Boolean, val value: List<VideoSearchResultEntity>) : Serializable {
    companion object {
        val EMPTY = BaseSearchResultEntity(false, emptyList())
    }
}