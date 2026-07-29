package com.example.kyrgyzkeyboard.ui.dictionary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kyrgyzkeyboard.data.model.WordEntity
import com.example.kyrgyzkeyboard.data.repository.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 字典管理 ViewModel。
 *
 * 数据源：Room（种子词表首启灌入 + 用户自学习/新增词），与键盘联想同源。
 *
 * 状态：
 * - [words]          词库全量列表（按频次+字典序）
 * - [searchQuery]    当前搜索文本
 * - [categories]     全部分类（含「全部」）
 * - [selectedCategory] 当前分类筛选（null=全部）
 * - [filteredWords]  搜索词 + 分类筛选后的列表
 * - [totalCount]     词条总数
 */
@HiltViewModel
class DictionaryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: DictionaryRepository
) : ViewModel() {

    private val _words = MutableStateFlow<List<WordEntity>>(emptyList())
    val words: StateFlow<List<WordEntity>> = _words.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    val filteredWords: StateFlow<List<WordEntity>> =
        combine(_words, _searchQuery, _selectedCategory) { list, query, category ->
            val q = query.trim()
            list.asSequence()
                .filter { category == null || it.category == category }
                .filter { q.isEmpty() || it.matches(q) }
                .toList()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> = _words
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    init {
        viewModelScope.launch {
            repository.seedIfNeeded(appContext)
            refresh()
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun addWord(word: WordEntity) {
        viewModelScope.launch {
            repository.addWord(
                word = word.word,
                pinyin = word.pinyin,
                meaning = word.meaning,
                category = word.category
            )
            refresh()
        }
    }

    fun updateWord(word: WordEntity) {
        viewModelScope.launch {
            repository.updateWord(word)
            refresh()
        }
    }

    fun deleteWord(id: Long) {
        viewModelScope.launch {
            repository.deleteWord(id)
            refresh()
        }
    }

    private suspend fun refresh() {
        _words.value = repository.getAllWords()
        _categories.value = repository.categories()
    }

    private fun WordEntity.matches(query: String): Boolean {
        val q = query.lowercase()
        return word.lowercase().contains(q) ||
            (pinyin?.lowercase()?.contains(q) == true) ||
            (meaning?.lowercase()?.contains(q) == true)
    }
}

