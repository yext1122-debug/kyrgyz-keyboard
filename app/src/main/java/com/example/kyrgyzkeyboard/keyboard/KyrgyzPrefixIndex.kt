package com.example.kyrgyzkeyboard.keyboard

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 柯语词库前缀索引（Trie）。
 *
 * - [put] / [increment] 写入词与频次（线程安全，写锁）。
 * - [query] 按前缀返回候选，频次降序、词长升序、字典序升序（读锁）。
 * - 纯 Kotlin，无 Android 依赖，便于替换词表或后续单测。
 */
class KyrgyzPrefixIndex {

    private class Node {
        val children = HashMap<Char, Node>()
        var word: String? = null
        var frequency: Int = 0
    }

    private val root = Node()
    private val lock = ReentrantReadWriteLock()

    @Volatile
    var size: Int = 0
        private set

    /** 写入或抬升某词频次到至少 [frequency]（取较大值，用于种子词表加载）。 */
    fun put(word: String, frequency: Int) {
        val w = word.trim()
        if (w.isEmpty()) return
        lock.write {
            val node = descendCreating(w)
            if (node.word == null) {
                node.word = w
                size++
            }
            if (frequency > node.frequency) node.frequency = frequency
        }
    }

    /** 频次累加 [delta]（词不存在则创建），返回累加后的频次。用于自学习。 */
    fun increment(word: String, delta: Int = 1): Int {
        val w = word.trim()
        if (w.isEmpty()) return 0
        lock.write {
            val node = descendCreating(w)
            if (node.word == null) {
                node.word = w
                size++
            }
            node.frequency += delta
            return node.frequency
        }
    }

    /**
     * 按前缀查询候选词，排除与前缀完全相同的词（由调用方另行补充）。
     * 结果按频次降序、词长升序、字典序升序，最多 [limit] 条。
     */
    fun query(prefix: String, limit: Int): List<String> {
        val p = prefix.trim()
        if (p.isEmpty() || limit <= 0) return emptyList()
        lock.read {
            val start = descend(p) ?: return emptyList()
            val matches = ArrayList<Node>()
            collect(start, matches)
            if (matches.isEmpty()) return emptyList()
            return matches
                .asSequence()
                .filter { it.word != null && it.word != p }
                .sortedWith(
                    compareByDescending<Node> { it.frequency }
                        .thenBy { it.word!!.length }
                        .thenBy { it.word }
                )
                .map { it.word!! }
                .take(limit)
                .toList()
        }
    }

    private fun descendCreating(word: String): Node {
        var node = root
        for (ch in word) {
            node = node.children.getOrPut(ch) { Node() }
        }
        return node
    }

    private fun descend(prefix: String): Node? {
        var node = root
        for (ch in prefix) {
            node = node.children[ch] ?: return null
        }
        return node
    }

    private fun collect(node: Node, out: MutableList<Node>) {
        if (node.word != null) out.add(node)
        for (child in node.children.values) collect(child, out)
    }
}
