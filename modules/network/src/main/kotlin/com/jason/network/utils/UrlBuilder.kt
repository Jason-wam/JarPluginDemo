package com.jason.network.utils

import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder

class UrlBuilder {
    private var host: String = ""
    private var path = ""
    private var params = HashMap<String, String>()
    private var charset = "utf-8"

    fun host(host: String): UrlBuilder {
        this.host = host
        return this
    }

    fun charset(charset: String): UrlBuilder {
        this.charset = charset
        return this
    }

    fun path(value: String): UrlBuilder {
        path = if (host.endsWith("/")) {
            value.removePrefix("/")
        } else {
            "/" + value.removePrefix("/")
        }
        return this
    }

    fun param(key: String, value: Any): UrlBuilder {
        params[key] = value.toString()
        return this
    }

    fun abs(second: String): UrlBuilder {
        if (second.startsWith("http")) {
            host = second
        } else {
            try {
                host = URL(URL(host), second).toString()
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }
        return this
    }

    private fun String.encode(charset: String): String {
        return URLEncoder.encode(this, charset)
    }

    fun build(): String {
        return buildString {
            append(host)
            append(path)
            params.forEach {
                if (contains("?")) {
                    append("&").append(it.key).append("=").append(it.value.encode(charset))
                } else {
                    append("?").append(it.key).append("=").append(it.value.encode(charset))
                }
            }
        }
    }
}