package com.jason.searcher.preference

abstract class ValueSelectPreference(val title: String, val summary: String, val values: List<String>) :
    BasePreference() {
    abstract fun getSelectedIndex(): Int
    abstract fun onSelectedIndexChange(index: Int)
}