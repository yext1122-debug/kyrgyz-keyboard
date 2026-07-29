package com.example.kyrgyzkeyboard.keyboard

import android.content.Context
import com.example.kyrgyzkeyboard.data.local.DictionaryDatabase
import com.example.kyrgyzkeyboard.data.model.WordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * [KyrgyzWordProvider] 的 Service 侧实现。
 *
 * 管线：assets 种子词表 + Room 自学习词 -> [KyrgyzPrefixIndex]（Trie）内存索引。
 * - [query] 走 Trie 前缀查询（频次降序），快、同步。
 * - [learn] 抬升内存频次并异步落库 Room。
 *
 * 纯本地，无网络。可在主线程构造（加载在 IO 协程异步进行）。
 */
class KyrgyzDictionaryStore(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : KyrgyzWordProvider {

    private val appContext = context.applicationContext
    private val dao = DictionaryDatabase.getInstance(appContext).wordDao()

    private val index = KyrgyzPrefixIndex()

    @Volatile
    private var loaded = false

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        // 1) assets 种子词表（词形 [\\t 频次] [\\t 释义]）
        runCatching {
            appContext.assets.open(ASSET_NAME).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    KyrgyzWordlistImporter.parse(reader).forEach { entry ->
                        index.put(entry.word, entry.frequency)
                    }
                }
            }
        }
        // 2) Room 自学习词（在种子基础上累加频次）
        runCatching {
            dao.getAll().forEach { entity ->
                val w = entity.word.trim()
                if (w.isNotEmpty()) index.increment(w, entity.frequency)
            }
        }
        loaded = true
    }

    override fun query(prefix: String): List<String> {
        if (!loaded) return emptyList()
        return index.query(prefix, MAX_CANDIDATES)
    }

    override fun learn(word: String) {
        val w = word.trim()
        if (w.isEmpty()) return
        index.increment(w, 1)
        scope.launch {
            runCatching {
                val existing = dao.findExact(w)
                if (existing != null) {
                    dao.incrementFrequency(existing.id)
                } else {
                    dao.insert(WordEntity(word = w, category = CATEGORY_USER, frequency = 1))
                }
            }
        }
    }

    companion object {
        private const val ASSET_NAME = "kyrgyz_words.txt"
        private const val CATEGORY_USER = "user"
        private const val MAX_CANDIDATES = 8
    }
}
