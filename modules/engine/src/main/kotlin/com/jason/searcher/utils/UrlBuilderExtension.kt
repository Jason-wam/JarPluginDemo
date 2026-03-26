package com.jason.searcher.utils

import com.jason.network.utils.UrlBuilder

fun buildUrl(apply: UrlBuilder.() -> Unit):String{
    return UrlBuilder().apply(apply).build()
}