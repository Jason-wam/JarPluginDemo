package com.jason.searcher.utils.documents.extension

import org.jsoup.Jsoup

fun String.htmlToString(): String {
    return Jsoup.parse(this).text()
}

/**
 * 解决某些table子项td无法识别的问题
 */
fun String.formatTableAsHtml(): String {
    var html = this
    if (html.endsWith("</td>")) {
        html = "<tr>${html}</tr>"
    }
    if (html.endsWith("</tr>") || html.endsWith("</tbody>")) {
        html = "<table>${html}</table>"
    }
    return html
}