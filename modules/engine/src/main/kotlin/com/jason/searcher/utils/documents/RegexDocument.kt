package com.jason.searcher.utils.documents

import com.jason.searcher.utils.documents.extension.htmlToString

internal class RegexDocument(private val html: String) : BaseDocument() {
    //规则##索引##属性##属性....
    override fun getElement(selector: String): String {
        val newRule = selector.removePrefix("@Regex:").removePrefix("/").removeSuffix("/")
        val matchResults = newRule.getRule().toRegex().findAll(html).toList()
        return matchResults.attr(newRule)
    }

    //规则##索引##属性##属性.... ，获取列表时将忽略索引
    override fun getElements(selector: String): List<String> {
        val rule = selector.split(">").map { it.trim() }.first()
        val newRule = rule.removePrefix("@Regex:").removePrefix("/").removeSuffix("/")
        val matchResults = newRule.getRule().toRegex().findAll(html).toList()
        return matchResults.attrList(newRule).let {
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
        return split("##").map {
            it.trim()
        }.find {
            it.startsWith("@range")
        }!!.removePrefix("@range(").removeSuffix(")").let {
            it.split("..").let { values ->
                IntRange(values[0].toInt(), values[1].ifEmpty { Int.MAX_VALUE.toString() }.toInt())
            }
        }
    }

    //正则>属性
    private fun String.getRule(): String {
        return split("##").first().trim()
    }

    private fun String.getIndex(): Int? {
        return split("##").getOrNull(1).let {
            when (it) {
                "first" -> 0
                "second" -> 1
                "third" -> 2
                "last" -> Int.MAX_VALUE
                else -> it?.toIntOrNull()
            }
        }
    }

    private fun String.getAttrs(): List<String> {
        return split("##").filterIndexed { index, _ ->
            index > 1
        }
    }

    private fun List<MatchResult>.attr(rule: String): String {
        val index = rule.getIndex() ?: 0
        val groupValues = lastOrNull()?.groupValues ?: return ""
        val result = if (index == Int.MAX_VALUE) {
            groupValues.lastOrNull() ?: return ""
        } else {
            groupValues.getOrNull(index) ?: return ""
        }
        return rule.getAttrs().fold(result) { accumulator, attr ->
            accumulator.attr(attr)
        }
    }

    private fun List<MatchResult>.attrList(rule: String): List<String> {
        val index = rule.getIndex()
        if (index == null) {
            return map {
                rule.getAttrs().fold(it.value) { accumulator, attr ->
                    accumulator.attr(attr)
                }
            }
        }
        return filter {
            it.groupValues.getOrNull(index) != null
        }.map {
            rule.getAttrs().fold(it.groupValues[index]) { accumulator, attr ->
                accumulator.attr(attr)
            }
        }
    }

    private fun String.attr(attr: String): String {
        if (attr == "trim()") {
            return trim()
        }
        if (attr == "uppercase()") {
            return uppercase()
        }
        if (attr == "lowercase()") {
            return lowercase()
        }
        if (attr == "htmlToString()") {
            return htmlToString()
        }
        if (attr.startsWith("clearText(")) {
            val replace = attr.removePrefix("clearText(").removeSuffix(")")
            val replacements = replace.split("|")
            return replacements.fold(this) { aac, item ->
                aac.replace(item, "")
            }
        }
        if (attr.startsWith("replaceText(")) {
            //@replace(Android>>IOS|Windows>>Macos)
            val replace = attr.removePrefix("replaceText(").removeSuffix(")")
            val replacements = replace.split("|")
            return replacements.fold(this) { aac, item ->
                val r = item.split(">>")
                if (r.size == 2) {
                    aac.replace(r[0], r[1])
                } else {
                    aac
                }
            }
        }
        return this
    }
}