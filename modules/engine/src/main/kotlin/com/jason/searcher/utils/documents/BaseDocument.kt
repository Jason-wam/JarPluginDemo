package com.jason.searcher.utils.documents

internal abstract class BaseDocument {
    abstract fun getElement(selector: String): String

    abstract fun getElements(selector: String): List<String>
}