package com.jason.searcher.utils

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.toMap(): Map<String, String> {
    return buildMap {
        for (key in keys()) {
            put(key, getString(key))
        }
    }
}

fun JSONObject.toPairs(): List<Pair<String, String>> {
    return buildList {
        for (key in keys()) {
            add(key to getString(key))
        }
    }
}

fun <T> JSONObject.map(block: (key: String, value: String) -> T): List<T> {
    return buildList {
        for (key in keys()) {
            add(block(key, getString(key)))
        }
    }
}

fun JSONObject.forEach(block: (key: String, value: String) -> Unit) {
    for (key in keys()) {
        block(key, getString(key))
    }
}

fun JSONObject.putIfNotEmpty(key: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        put(key, value)
    }
}

inline fun <reified T, R> JSONArray.toList(block: (T) -> R): List<R> {
    return buildList {
        for (i in 0 until this@toList.length()) {
            val item = this@toList.get(i)
            if (item is T) {
                add(block(item))
            }
        }
    }
}