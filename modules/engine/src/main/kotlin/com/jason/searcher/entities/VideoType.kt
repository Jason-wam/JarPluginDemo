package com.jason.searcher.entities

enum class VideoType {
    /**
     * 直链播放链接，APP直接播放
     */
    Direct,

    /**
     * 播放页链接，app自动抓取播放链接
     */
    Sniff,

    /**
     * 播放页链接，通过Jar内部getPlayUrl方法获取视频链接
     */
    Decode,

    /**
     * 详情页链接，app内部跳转新的页面
     */
    DetailPage
}