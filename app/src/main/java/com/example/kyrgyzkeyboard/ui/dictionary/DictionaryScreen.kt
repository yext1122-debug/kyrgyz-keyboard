package com.example.kyrgyzkeyboard.ui.dictionary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kyrgyzkeyboard.R
import com.example.kyrgyzkeyboard.data.model.WordEntity

/**
 * 词库管理页（与键盘联想同源：Room）。
 *
 * - 顶部：标题栏（含「多选」入口）+ 搜索框 + 词条计数 + 分类筛选 Chip
 * - 主体：LazyColumn 展示词条（词形 / 释义 / 频次 / 内置·自学习标签）
 * - 交互：
 *   - 普通模式：点按编辑（编辑对话框内含删除），FAB 新增
 *   - 多选模式：行首多选框，支持全选 / 取消全选 / 删除选中
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(viewModel: DictionaryViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val items by viewModel.filteredWords.collectAsState()
    val total by viewModel.totalCount.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WordEntity?>(null) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                id = R.string.dictionary_selected_count,
                                selectedIds.size
                            )
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = { viewModel.toggleMultiSelectMode() }) {
                            Text(text = stringResource(id = R.string.dictionary_cancel_multi_select))
                        }
                    },
                    actions = {
                        val allSelected = total > 0 && selectedIds.size >= total
                        TextButton(
                            onClick = {
                                if (allSelected) viewModel.deselectAll() else viewModel.selectAll()
                            }
                        ) {
                            Text(
                                text = stringResource(
                                    id = if (allSelected) {
                                        R.string.dictionary_deselect_all
                                    } else {
                                        R.string.dictionary_select_all
                                    }
                                )
                            )
                        }
                        TextButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = { showDeleteSelectedDialog = true }
                        ) {
                            Text(
                                text = stringResource(id = R.string.dictionary_delete_selected),
                                color = if (selectedIds.isNotEmpty()) {
                                    Color(0xFFB00020)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text(text = stringResource(id = R.string.dictionary_title)) },
                    actions = {
                        TextButton(
                            enabled = total > 0,
                            onClick = { viewModel.toggleMultiSelectMode() }
                        ) {
                            Text(text = stringResource(id = R.string.dictionary_multi_select))
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Text(text = "＋", fontSize = 22.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(text = stringResource(id = R.string.search_hint)) },
                singleLine = true
            )

            Text(
                text = stringResource(id = R.string.dictionary_count, total, items.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text(text = stringResource(id = R.string.dictionary_category_all)) }
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(text = category) }
                        )
                    }
                }
            }

            Divider()

            when {
                total == 0 -> EmptyState(text = stringResource(id = R.string.empty_dictionary))
                items.isEmpty() -> EmptyState(text = stringResource(id = R.string.dictionary_no_result))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = items, key = { it.id }) { entity ->
                        WordRow(
                            word = entity,
                            multiSelect = isMultiSelectMode,
                            selected = selectedIds.contains(entity.id),
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSelection(entity.id)
                                } else {
                                    editing = entity
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        WordDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { new ->
                viewModel.addWord(new)
                showAddDialog = false
            },
            onDelete = null
        )
    }

    editing?.let { current ->
        WordDialog(
            initial = current,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                viewModel.updateWord(updated.copy(id = current.id, frequency = current.frequency))
                editing = null
            },
            onDelete = {
                viewModel.deleteWord(current.id)
                editing = null
            }
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(text = stringResource(id = R.string.dictionary_delete_selected)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.dictionary_delete_selected_confirm,
                        selectedIds.size
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    showDeleteSelectedDialog = false
                }) {
                    Text(
                        text = stringResource(id = R.string.dictionary_delete),
                        color = Color(0xFFB00020)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(text = stringResource(id = R.string.delete_confirm_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordRow(
    word: WordEntity,
    multiSelect: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (multiSelect) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            if (!word.meaning.isNullOrBlank()) {
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SuggestionChip(
                onClick = {},
                enabled = false,
                label = { Text(text = word.category, fontSize = 11.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
            Text(
                text = stringResource(id = R.string.dictionary_freq, word.frequency),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordDialog(
    initial: WordEntity?,
    onDismiss: () -> Unit,
    onConfirm: (WordEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var word by remember { mutableStateOf(initial?.word.orEmpty()) }
    var pinyin by remember { mutableStateOf(initial?.pinyin.orEmpty()) }
    var meaning by remember { mutableStateOf(initial?.meaning.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: "常用") }

    val titleRes = if (initial == null) R.string.add_word else R.string.dictionary_edit_word

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text(text = "柯尔克孜语") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pinyin,
                    onValueChange = { pinyin = it },
                    label = { Text(text = "拼音") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text(text = "释义") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(text = "分类") },
                    singleLine = true
                )
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.dictionary_delete),
                            color = Color(0xFFB00020)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = word.isNotBlank(),
                onClick = {
                    onConfirm(
                        WordEntity(
                            word = word.trim(),
                            pinyin = pinyin.trim().ifEmpty { null },
                            meaning = meaning.trim().ifEmpty { null },
                            category = category.trim().ifEmpty { "常用" }
                        )
                    )
                }
            ) { Text(text = stringResource(id = R.string.done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.delete_confirm_cancel))
            }
        }
    )

    Spacer(modifier = Modifier.height(0.dp))
}
