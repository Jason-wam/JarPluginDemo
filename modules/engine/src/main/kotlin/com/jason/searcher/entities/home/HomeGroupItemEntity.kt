package com.jason.searcher.entities.home

import com.jason.searcher.entities.VideoCoverRatio
import java.io.Serializable

class HomeGroupItemEntity : Serializable {
    var image: String = ""
    var imageRatio: VideoCoverRatio = VideoCoverRatio.Auto
    var title: String = ""
    var subtitle: String = ""
    var score:String =  ""

    var data: String = ""
    var dataType: HomeItemDataType = HomeItemDataType.SEARCH_WORDS
}