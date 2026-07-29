package com.example.kyrgyzkeyboard.keyboard

import android.content.Context
import com.example.kyrgyzkeyboard.data.local.DictionaryDatabase
import com.example.kyrgyzkeyboard.data.model.WordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Service-side implementation of [KyrgyzWordProvider].
 *
 * - Loads a seed wordlist from assets and any learned words from Room into memory.
 * - [query] does an in-memory prefix filter ordered by frequency (fast, sync).
 * - [learn] bumps the in-memory frequency and persists to Room asynchronously.
 *
 * Purely local; no network. Safe to construct on the main thread (loading is async).
 */
class KyrgyzDictionaryStore(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : KyrgyzWordProvider {

    private val appContext = context.applicationContext
    private val dao = DictionaryDatabase.getInstance(appContext).wordDao()

    // word -> frequency
    private val freq = ConcurrentHashMap<String, Int>()

    @Volatile
    private var loaded = false

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        // 1) seed list from assets
        runCatching {
            appContext.assets.open(ASSET_NAME).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).forEachLine { raw ->
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("#")) return@forEachLine
                    val parts = line.split('\t')
                    val word = parts[0].trim()
                    if (word.isEmpty()) return@forEachLine
                    val f = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                    freq[word] = maxOf(freq[word] ?: 0, f)
                }
            }
        }
        // 2) learned words from Room (accumulate on top of seed)
        runCatching {
            dao.getAll().forEach { entity ->
                val w = entity.word.trim()
                if (w.isNotEmpty()) {
                    freq[w] = (freq[w] ?: 0) + entity.frequency
                }
            }
        }
        loaded = true
    }

    override fun query(prefix: String): List<String> {
        val p = prefix.trim()
        if (p.isEmpty() || !loaded) return emptyList()
        return freq.entries
            .asSequence()
            .filter { it.key.startsWith(p) && it.key != p }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length }.thenBy { it.key })
            .map { it.key }
            .take(MAX_CANDIDATES)
            .toList()
    }

    override fun learn(word: String) {
        val w = word.trim()
        if (w.isEmpty()) return
        freq[w] = (freq[w] ?: 0) + 1
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