package com.jason.searcher.utils.documents

import com.jayway.jsonpath.JsonPath

internal class JsonDocument(json: String) : BaseDocument() {
    private val document = JsonPath.parse(json)

    override fun getElement(selector: String): String {
        if (selector.isBlank()) return ""
        val rule = selector.split(">").first().trim()
        val newRule = rule.replace("@Json:", "$.")
        val attr = selector.substringAfter(">")
        return document.read<Any>(newRule).toString().resolveAttr(attr)
    }

    override fun getElements(selector: String): List<String> {
        if (selector.isBlank()) return emptyList()
        val rule = selector.split(">").first().trim()
        val newRule = rule.replace("@Json:", "$.")
        if (newRule == "") return emptyList()
        val result = document.read<Any>(newRule)
        if (document.configuration().jsonProvider().isArray(result)) {
            result as List<*>
            return result.map {
                try {
                    document.configuration().jsonProvider().toJson(it)
                } catch (_: Exception) {
                    it.toString()
                }
            }.let {
                val indexRange = selector.getIndexRange()
                if (indexRange.last == Int.MAX_VALUE || indexRange.last >= it.lastIndex) {
                    it.slice(indexRange.first..it.lastIndex)
                } else {
                    it.slice(indexRange)
                }
            }
        }
        return arrayListOf(result.toString())
    }

    private fun String.getIndexRange(): IntRange {
        if (!contains("@range")) {
            return IntRange(0, Int.MAX_VALUE)
        }
        return split(">").map {
            it.trim()
        }.find {
            it.startsWith("@range")
        }!!.removePrefix("@range(").removeSuffix(")").let {
            it.split("..").let { values ->
                IntRange(values[0].toInt(), values[1].ifEmpty { Int.MAX_VALUE.toString() }.toInt())
            }
        }
    }

    private fun String.resolveAttr(attrNodes: String): String {
        if(attrNodes.isEmpty()) return this
        val nodes = attrNodes.split(">").map { it.trim() }
        return nodes.fold(this) { txt, node ->
            when {
                node.startsWith("@trim") -> txt.trim()
                node.startsWith("@uppercase") -> txt.uppercase()
                node.startsWith("@lowercase") -> txt.lowercase()
                node.startsWith("@clearText") -> {
                    //@clear(需要清除的字符串)
                    val clear = node.removePrefix("@clearText(").removeSuffix(")")
                    clear.split("|").fold(txt) { aac, item ->
                        aac.replace(item, "")
                    }
                }

                node.startsWith("@replaceText") -> {
                    //@replace(Android,IOS)
                    val replace = node.removePrefix("@replaceText(").removeSuffix(")")
                    replace.split("|").fold(txt) { aac, item ->
                        val r = item.split(",")
                        if (r.size == 2) {
                            aac.replace(r[0], r[1])
                        } else {
                            aac
                        }
                    }
                }

                else -> txt
            }
        }
    }
}