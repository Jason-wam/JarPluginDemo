package com.jason.searcher.utils

import java.net.URI
import java.net.URL
import java.util.regex.Pattern

object HttpUtils {
    private val URL_HOST_PATTERN by lazy {
        Pattern.compile("^[a-zA-z0-9]+://\\S*?/")
    }

    fun isAbsoluteUrl(url: String): Boolean {
        return try {
            URI(url).isAbsolute
        } catch (_: Exception) {
            false
        }
    }

    fun buildAbsoluteUrl(baseUrl: String, url: String): String {
        if (isAbsoluteUrl(url)) return url
        return try {
            URL(URL(baseUrl), url).toString()
        } catch (_: Exception) {
            baseUrl.take(baseUrl.lastIndexOf("/") + 1) + url
        }
    }

    fun getHost(url: String): String {
        val matcher = URL_HOST_PATTERN.matcher(url)
        if (matcher.find()) {
            return matcher.group()
        }
        return ""
    }
}