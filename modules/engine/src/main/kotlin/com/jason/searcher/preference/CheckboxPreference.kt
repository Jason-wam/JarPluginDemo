package com.jason.searcher.preference

abstract class CheckboxPreference(val title: String, val summary: String) : BasePreference() {
    abstract fun isChecked(): Boolean
    abstract fun onCheckedStateChange(checked: Boolean)
}