package com.jason.network.cache

object CacheValidDuration {

    /**
     * 禁用缓存，无论什么缓存模式都不会写入缓存
     */
    const val NEVER = 0L

    /**
     * 永久缓存，不会失效
     */
    const val FOREVER = -1L
}