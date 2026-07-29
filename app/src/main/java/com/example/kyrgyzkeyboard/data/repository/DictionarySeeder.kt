package com.example.kyrgyzkeyboard.data.repository

import android.content.Context
import com.example.kyrgyzkeyboard.data.local.DictionaryDao
import com.example.kyrgyzkeyboard.data.model.WordEntity
import com.example.kyrgyzkeyboard.keyboard.KyrgyzWordlistImporter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 种子词表灌库器：把 assets 里的柯语词表导入 Room，作为词库唯一数据源。
 *
 * 设计：
 * - 用 SharedPreferences 记录已灌库的种子版本 [SEED_VERSION]；版本不同才重新灌库，
 *   因此替换 assets 词表后只需把 [SEED_VERSION] +1，即可在下次启动自动灌入新词。
 * - 批量插入用 INSERT OR IGNORE（见 DAO），已存在的词形不会覆盖用户频次/自学习结果。
 * - 幂等：可从 App 进程与输入法 Service 侧重复调用，只会真正灌库一次。
 */
object DictionarySeeder {

    /** 种子词表版本；替换 assets/kyrgyz_words.txt 后请 +1 触发重新灌库。 */
    const val SEED_VERSION = 1

    private const val PREFS_NAME = "dictionary_seed"
    private const val KEY_SEEDED_VERSION = "seeded_version"
    private const val ASSET_NAME = "kyrgyz_words.txt"

    const val CATEGORY_BUILTIN = "内置"

    // 同进程内互斥，避免 App 与输入法 Service 并发灌库产生重复词条。
    private val mutex = Mutex()

    /** 若种子版本未灌入则灌入。进程内互斥 + 版本标记双重保证只灌一次。 */
    suspend fun seedIfNeeded(context: Context, dao: DictionaryDao) = mutex.withLock {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seeded = prefs.getInt(KEY_SEEDED_VERSION, 0)
        if (seeded >= SEED_VERSION) return@withLock

        val entries = runCatching {
            appContext.assets.open(ASSET_NAME).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    KyrgyzWordlistImporter.parse(reader)
                }
            }
        }.getOrDefault(emptyList())

        if (entries.isNotEmpty()) {
            val rows = entries.map { entry ->
                WordEntity(
                    word = entry.word,
                    meaning = entry.meaning,
                    category = CATEGORY_BUILTIN,
                    frequency = entry.frequency
                )
            }
            runCatching { dao.insertAll(rows) }
        }
        prefs.edit().putInt(KEY_SEEDED_VERSION, SEED_VERSION).apply()
    }
}

