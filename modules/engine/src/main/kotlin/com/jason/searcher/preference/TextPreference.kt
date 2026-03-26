package com.jason.searcher.preference

abstract class TextPreference(
    val title: String,
    val summary: String = "",
    val endText: String = "",
    val showMoreConner: Boolean = false
) : BasePreference(){
    abstract fun onClick()
}