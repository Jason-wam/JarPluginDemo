package com.jason.searcher.preference

abstract class TextEditPreference(val title: String, val summary: String, val hint: String) : BasePreference() {
    abstract fun getText(): String
    abstract fun onTextChanged(text: String)
}