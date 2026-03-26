package com.jason.searcher.preference

abstract class ButtonPreference(val text: String) : BasePreference(){
    abstract fun onClick()
}