package com.jason.searcher.entities.home

import java.io.Serializable

class HomeRecommendsEntity : Serializable {
    val banners = arrayListOf<HomeBannerEntity>()
    val plugins = arrayListOf<HomePluginEntity>()
    val groups = arrayListOf<HomeGroupEntity>()
}