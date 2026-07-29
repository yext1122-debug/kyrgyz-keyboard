package com.example.kyrgyzkeyboard.keyboard

import java.io.BufferedReader

/**
 * 柯语种子词表解析器。
 *
 * 词表格式（UTF-8，每行一条，制表符或空白分隔）：
 *   词形 [\\t 频次] [\\t 释义]
 * - 以 # 开头或空行忽略（注释）。
 * - 频次缺省为 0；非法频次按 0 处理。
 * - 仅解析为结构化数据，不做存储；由调用方写入索引/Room。
 */
object KyrgyzWordlistImporter {

    /** 一条已解析的词表项。 */
    data class Entry(val word: String, val frequency: Int, val meaning: String?)

    /** 逐行解析词表流。非法行被跳过，不抛异常。 */
    fun parse(reader: BufferedReader): List<Entry> {
        val out = ArrayList<Entry>()
        reader.forEachLine { raw ->
            val entry = parseLine(raw) ?: return@forEachLine
            out.add(entry)
        }
        return out
    }

    /** 解析单行；返回 null 表示该行应被忽略。 */
    fun parseLine(raw: String): Entry? {
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) return null
        // 优先按制表符分列；无制表符时退化为按首个空白切出词形。
        val parts = if (line.contains('\t')) {
            line.split('\t')
        } else {
            line.split(Regex("\\s+"), limit = 2)
        }
        val word = parts.getOrNull(0)?.trim().orEmpty()
        if (word.isEmpty()) return null
        val frequency = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val meaning = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
        return Entry(word = word, frequency = frequency, meaning = meaning)
    }
}
