package ru.mtuci.drivenext.data

import java.net.URLEncoder

/** Кавычки защищают синтаксис фильтра PostgREST, URL-кодирование — сам URL. */
internal object CarSearchQuery {
    fun filter(query: String): String {
        val text = query.trim()
        if (text.isEmpty()) return ""
        val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
        val pattern = "\"%$escaped%\""
        val expression = "(brand.ilike.$pattern,model.ilike.$pattern)"
        return "&or=" + URLEncoder.encode(expression, "UTF-8").replace("+", "%20")
    }
}
