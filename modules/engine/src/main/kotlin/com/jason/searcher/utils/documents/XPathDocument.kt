package com.jason.searcher.utils.documents

import com.jason.searcher.utils.documents.extension.formatTableAsHtml
import org.jsoup.Jsoup
import org.seimicrawler.xpath.JXDocument

internal class XPathDocument(url: String, html: String) : BaseDocument() {
    private val documents: JXDocument = JXDocument.create(
        Jsoup.parse(
            html.formatTableAsHtml(), url
        )
    )

    override fun getElement(selector: String): String {
        if (selector.isBlank()) return ""
        val newRule = selector.getSelector()
        val attrsNodes = selector.getAttr()

        val results = documents.selN(newRule)
        if (results.isEmpty()) return ""
        if (results.size == 1) {
            return results.first().toString().resolveAttr(attrsNodes)
        }

        val attrsNodesList = attrsNodes.split(">").map { it.trim() }
        if (attrsNodesList.first().startsWith("@index")) {
            val index = attrsNodesList.first().removePrefix("@index(").removeSuffix(")")
            val otherNodes = attrsNodesList.filterIndexed { index, string -> index > 0 }
            return if (otherNodes.isEmpty()) {
                results[index.toInt()].toString()
            } else {
                results[index.toInt()].toString().resolveAttr(otherNodes.joinToString(">"))
            }
        }
        if (attrsNodesList.first().startsWith("@first")) {
            val index = 0
            val otherNodes = attrsNodesList.filterIndexed { index, string -> index > 0 }
            return if (otherNodes.isEmpty()) {
                results[index].toString()
            } else {
                results[index].toString().resolveAttr(otherNodes.joinToString(">"))
            }
        }
        if (attrsNodesList.first().startsWith("@second")) {
            val index = 1
            val otherNodes = attrsNodesList.filterIndexed { index, _ -> index > 0 }
            return if (otherNodes.isEmpty()) {
                results[index].toString()
            } else {
                results[index].toString().resolveAttr(otherNodes.joinToString(">"))
            }
        }
        if (attrsNodesList.first().startsWith("@third")) {
            val index = 2
            val otherNodes = attrsNodesList.filterIndexed { index, _ -> index > 0 }
            return if (otherNodes.isEmpty()) {
                results[index].toString()
            } else {
                results[index].toString().resolveAttr(otherNodes.joinToString(">"))
            }
        }
        if (attrsNodesList.first().startsWith("@last")) {
            val otherNodes = attrsNodesList.filterIndexed { index, _ -> index > 0 }
            return if (otherNodes.isEmpty()) {
                results.last().toString()
            } else {
                results.last().toString().resolveAttr(otherNodes.joinToString(">"))
            }
        }

        return results.joinToString("\n") { it.toString() }
    }

    override fun getElements(selector: String): List<String> {
        val rule = selector.split(">").first().trim()
        val innerRule = rule.removePrefix("@XPath:")
        if (selector.isBlank()) return emptyList()
        return documents.selN(innerRule).map {
            it.toString()
        }.let {
            val indexRange = selector.getIndexRange()
            if (indexRange.last == Int.MAX_VALUE || indexRange.last >= it.lastIndex) {
                it.slice(indexRange.first..it.lastIndex)
            } else {
                it.slice(indexRange)
            }
        }
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

    private fun String.getSelector(): String {
        return split(">").first().trim().removePrefix("@XPath:")
    }

    private fun String.getAttr(): String {
        if (!contains(">")) return ""
        return substringAfter(">")
    }

    private fun String.resolveAttr(attrNodes: String): String {
        if (attrNodes.isEmpty()) return this
        val nodes = attrNodes.split(">").map { it.trim() }
        return nodes.fold(this) { txt, node ->
            when {
                node.startsWith("@uppercase") -> txt.uppercase()
                node.startsWith("@lowercase") -> txt.lowercase()
                node.startsWith("@trim") -> txt.trim()
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