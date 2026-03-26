package com.jason.searcher.entities.home

import java.io.Serializable

class HomeBannerEntity: Serializable {
    var image: String = ""
    var title: String = ""
    var subtitle: String = ""

    var data: String = ""
    var dataType: HomeItemDataType = HomeItemDataType.SEARCH_WORDS
}