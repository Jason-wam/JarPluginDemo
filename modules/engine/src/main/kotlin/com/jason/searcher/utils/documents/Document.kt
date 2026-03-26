package com.jason.searcher.utils.documents

class Document(val url: String, val html: String) {

    companion object {
        fun from(baseUrl: String, html: String) = Document(baseUrl, html)
    }

    fun getElement(selector: String): String {
        try {
            return when {
                selector.startsWith("@Raw:") -> selector.removePrefix("@Raw:")

                selector.startsWith("@Url") -> url

                selector.startsWith("@Html") -> html

                selector.startsWith("@Json:") || selector.startsWith("$.") -> {
                    JsonDocument(html).getElement(selector)
                }

                selector.startsWith("@Regex:") -> {
                    RegexDocument(html).getElement(selector)
                }

                selector.startsWith("@XPath:") || selector.startsWith("//") || selector.startsWith("/") -> {
                    XPathDocument(url, html).getElement(selector)
                }

                else -> {
                    JsoupDocument(url, html).getElement(selector)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun getElements(selector: String): List<String> {
        try {
            if (selector.startsWith("@Raw:")) {
                return selector.removePrefix("@Raw:").split("&&")
            }
            if (selector == "@Url") return arrayListOf(url)
            if (selector == "@Data") return arrayListOf(html)

            if (selector.contains("||")) {
                val rules = selector.split("||")
                for (rule in rules) {
                    val results = getResults(rule)
                    if (results.isNotEmpty()) {
                        return results
                    }
                }
                return emptyList()
            } else if (selector.contains("&&")) {
                return ArrayList<String>().apply {
                    val rules = selector.split("&&")
                    for (rule in rules) {
                        addAll(getResults(rule))
                    }
                }
            } else {
                return getResults(selector)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun getResults(selector: String): List<String> {
        var newRule = selector
        var reverse = false
        if (newRule.startsWith("@Reversed:")) {
            newRule = newRule.removePrefix("@Reversed:")
            reverse = true
        }
        when {
            newRule.startsWith("@Raw:") -> {
                return newRule.removePrefix("@Raw:").split("&&").let {
                    if (reverse) {
                        it.reversed()
                    } else {
                        it
                    }
                }
            }

            newRule.startsWith("@Url") -> return arrayListOf(url)

            newRule.startsWith("@Data") -> return arrayListOf(html)

            newRule.startsWith("@XPath:") || newRule.startsWith("//") || selector.startsWith("/") -> {
                return XPathDocument(url, html).getElements(newRule).let {
                    if (reverse) {
                        it.reversed()
                    } else {
                        it
                    }
                }
            }

            newRule.startsWith("@Json:") || newRule.startsWith("$.") -> {
                return JsonDocument(html).getElements(newRule).let {
                    if (reverse) {
                        it.reversed()
                    } else {
                        it
                    }
                }
            }

            newRule.startsWith("@Regex:") -> {
                return RegexDocument(html).getElements(newRule).let {
                    if (reverse) {
                        it.reversed()
                    } else {
                        it
                    }
                }
            }

            else -> {
                return JsoupDocument(url, html).getElements(newRule).let {
                    if (reverse) {
                        it.reversed()
                    } else {
                        it
                    }
                }
            }
        }
    }

}