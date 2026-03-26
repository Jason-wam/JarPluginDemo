package com.jason.searcher.utils.documents

import com.jason.searcher.utils.documents.extension.formatTableAsHtml
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

internal class JsoupDocument(url: String, html: String) : BaseDocument() {
    private val document = Jsoup.parse(html.formatTableAsHtml(), url)

    override fun getElement(selector: String): String {
        return document.selectElements(selector).first()?.resolveAttr(selector)?.resolveOtherAttr(selector) ?: ""
    }

    override fun getElements(selector: String): List<String> {
        return document.selectElements(selector).map {
            it.outerHtml() //列表不带属性，直接返回html
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

    private fun Document.selectElements(selector: String): Elements {
        if (selector.isBlank()) return Elements()
        if (!selector.contains("@")) return select(selector)
        var elements = Elements(allElements)
        val childRuleNodes = selector.split(">").map { it.trim() }
        for (ruleNode in childRuleNodes) {
            if (ruleNode.startsWith("@remove")) {
                //排除指定结果
                val ignore = ruleNode.removePrefix("@remove(").removeSuffix(")")
                elements = elements.select(ignore).remove()
            }
            if (ruleNode.startsWith("@first")) {
                //选择指定索引的结果 @index(0)
                elements = elements.getByIndex(0)
            }
            if (ruleNode.startsWith("@second")) {
                //选择指定索引的结果 @index(1)
                elements = elements.getByIndex(1)
            }
            if (ruleNode.startsWith("@third")) {
                //选择指定索引的结果 @index(2)
                elements = elements.getByIndex(2)
            }
            if (ruleNode.startsWith("@last")) {
                //选择指定索引的结果 @index(lastIndex)
                elements = elements.getByIndex(elements.lastIndex)
            }
            if (ruleNode.startsWith("@index")) {
                //选择指定索引的结果 @index(0)
                val value = ruleNode.removePrefix("@index(").removeSuffix(")")
                elements = if (!value.contains("..")) {
                    elements.getByIndex(value.toInt())
                } else {
                    elements.getByIndexRange(value.split("..").let {
                        IntRange(it[0].toInt(), it[1].toInt())
                    })
                }
            }
            if (!ruleNode.startsWith("@")) {
                elements = elements.select(ruleNode)
            }
        }
        return elements
    }

    private fun Elements.getByIndex(index: Int): Elements {
        return if (index in indices) {
            Elements(this[index])
        } else {
            Elements()
        }
    }

    private fun Elements.getByIndexRange(range: IntRange): Elements {
        return if (range.last >= lastIndex) {
            Elements(slice(range.first..lastIndex))
        } else {
            Elements(slice(range))
        }
    }

    private val otherNodes = buildList {
        add("@trim")
        add("@clearText")
        add("@replaceText")
        add("@uppercase")
        add("@lowercase")
    }

    private fun Element.resolveAttr(selector: String): String {
        val nodes = selector.split(">").map { it.trim() }
        val suffix = nodes.findLast { node ->
            !otherNodes.any { node.startsWith(it) }
        }
        if (suffix == null) return outerHtml()
        return when {
            suffix.startsWith("@attr") -> {
                //@attr(attrKey)
                val attr = suffix.removePrefix("@attr(").removeSuffix(")")
                attr(attr)
            }

            suffix.startsWith("@hasClass") -> {
                //@hasClass(className)
                val className = suffix.removePrefix("@hasClass(").removeSuffix(")")
                hasClass(className).toString()
            }

            suffix.startsWith("@hasAttr") -> {
                //@hasAttr(attrKey)
                val className = suffix.removePrefix("@hasAttr(").removeSuffix(")")
                hasAttr(className).toString()
            }

            suffix == "@id" -> id()
            suffix == "@text" -> text()
            suffix == "@html" -> html()
            suffix == "@data" -> data()
            suffix == "@hasText" -> hasText().toString()
            suffix == "@ownText" -> ownText()
            suffix == "@wholeText" -> wholeText()
            suffix == "@outerHtml" -> outerHtml()
            suffix == "@className" -> className()
            suffix == "@parents" -> parents().html()
            suffix == "@children" -> children().html()
            suffix == "@cssSelector" -> cssSelector()
            suffix == "@tagName" -> tagName()
            suffix == "@normalName" -> normalName()
            suffix == "@hasParent" -> hasParent().toString()
            suffix == "@childrenSize" -> childrenSize().toString()
            suffix == "@ownerDocument" -> ownerDocument()?.html().orEmpty()
            suffix == "@dataset" -> buildString {
                dataset().forEach {
                    appendLine(it)
                }
            }

            suffix == "@dataNodes" -> buildString {
                dataNodes().forEach {
                    appendLine(it)
                }
            }

            suffix == "@textNodes" -> buildString {
                textNodes().also {
                    it.forEachIndexed { index, textNode ->
                        append(textNode)
                        if (index != it.lastIndex) {
                            appendLine()
                        }
                    }
                }
            }

            suffix == "@childNodes" -> buildString {
                childNodes().forEach {
                    appendLine(it)
                }
            }

            suffix == "@linesText" -> buildString {
                textNodes().also {
                    it.forEachIndexed { index, textNode ->
                        if (!textNode.isBlank) {
                            append(textNode.text().trim())
                            if (index != it.lastIndex) {
                                appendLine()
                            }
                        }
                    }
                }
            }

            suffix == "@classNames" -> buildString {
                classNames().forEach {
                    appendLine(it)
                }
            }

            else -> outerHtml()
        }
    }

    private fun String.resolveOtherAttr(selector: String): String {
        val nodes = selector.split(">").map { it.trim() }
        val finalNodes = nodes.filter { node ->
            otherNodes.any { node.startsWith(it) }
        }
        if (finalNodes.isEmpty()) return this
        return finalNodes.fold(this) { text, node ->
            when {
                node.startsWith("@clearText") -> {
                    //@clear(需要清除的字符串)
                    val clear = node.removePrefix("@clearText(").removeSuffix(")")
                    clear.split("|").fold(text) { aac, item ->
                        aac.replace(item, "")
                    }
                }

                node.startsWith("@replaceText") -> {
                    //@replace(Android,IOS)
                    val replace = node.removePrefix("@replaceText(").removeSuffix(")")
                    replace.split("|").fold(text) { aac, item ->
                        val r = item.split(",")
                        if (r.size == 2) {
                            aac.replace(r[0], r[1])
                        } else {
                            aac
                        }
                    }
                }

                node.startsWith("@trim") -> {
                    text.trim()
                }

                node.startsWith("@uppercase") -> {
                    text.uppercase()
                }

                node.startsWith("@lowercase") -> {
                    text.lowercase()
                }

                else -> text
            }
        }
    }
}