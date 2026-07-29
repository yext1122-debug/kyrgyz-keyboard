package com.example.kyrgyzkeyboard.keyboard

import android.content.Context
import com.example.kyrgyzkeyboard.data.local.DictionaryDatabase
import com.example.kyrgyzkeyboard.data.model.WordEntity
import com.example.kyrgyzkeyboard.data.repository.DictionarySeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [KyrgyzWordProvider] 的 Service 侧实现。
 *
 * 管线：Room 词库（种子词表首启已灌入 + 用户自学习词）-> [KyrgyzPrefixIndex]（Trie）内存索引。
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
        // 首启把 assets 种子词表灌入 Room（幂等），保证词库唯一数据源。
        runCatching { DictionarySeeder.seedIfNeeded(appContext, dao) }
        // Room 词库（种子词 + 用户自学习词）一次性载入 Trie。
        runCatching {
            dao.getAll().forEach { entity ->
                val w = entity.word.trim()
                if (w.isNotEmpty()) index.put(w, entity.frequency)
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
        private const val CATEGORY_USER = "user"
        private const val MAX_CANDIDATES = 8
    }
}

