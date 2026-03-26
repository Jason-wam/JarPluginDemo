package com.jason.searcher.entities.home

import java.io.Serializable

/**
 * 首页推荐分组类容
 */
class HomeGroupEntity: Serializable {
    var title: String = ""
    var subtitle: String = ""
    var children: ArrayList<HomeGroupItemEntity> = arrayListOf()
}