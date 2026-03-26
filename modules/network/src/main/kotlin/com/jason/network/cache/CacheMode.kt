package com.jason.network.cache

enum class CacheMode {
    /**
     * 仅使用缓存数据
     */
    ONLY_CACHE,

    /**
     * 仅使用网络数据，如果请求成功会自动写入缓存
     */
    ONLY_NETWORK,

    /**
     * 优先使用缓存数据，如果缓存数据不可用则从网络获取，如果请求成功会自动写入缓存
     */
    CACHE_ELSE_NETWORK,

    /**
     * 优先从网络获取数据，如果网络数据不可用则使用缓存数据，如果请求成功会自动写入缓存
     */
    NETWORK_ELSE_CACHE
}