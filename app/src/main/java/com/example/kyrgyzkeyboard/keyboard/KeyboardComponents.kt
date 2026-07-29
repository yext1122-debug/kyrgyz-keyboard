package com.example.kyrgyzkeyboard.keyboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.kyrgyzkeyboard.ui.theme.KeyboardTokens

/**
 * 共享键盘渲染组件。
 *
 * IME 服务（真实输入）与预览页共用同一套按键绘制，避免两份重复实现。
 * 统一提供：按下反馈（变色 + 轻微放大 + 触觉回调）、长按变体气泡（可选任意变体）。
 *
 * 回调职责划分：
 * - [onKey]      普通点击：交给引擎处理并同步状态。
 * - [onVariant]  从长按气泡中选中某个变体字符。
 * - [onFeedback] 按下瞬间触发的触觉/听觉反馈（震动 / 按键声）。
 */
@Composable
fun KeyboardRowView(
    row: KeyboardRow,
    fontSize: Float,
    shiftActive: Boolean,
    shiftLocked: Boolean,
    onKey: (KyrgyzKey) -> Unit,
    onVariant: (String) -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: Int = KeyboardTokens.RowHeight.value.toInt(),
    enterLabel: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight.dp),
        horizontalArrangement = Arrangement.spacedBy(KeyboardTokens.KeyGap)
    ) {
        row.keys.forEachIndexed { index, k ->
            androidx.compose.runtime.key(index, k.code, k.function) {
                KeyboardKeyView(
                    keyData = k,
                    fontSize = fontSize,
                    shiftActive = shiftActive,
                    shiftLocked = shiftLocked,
                    onKey = onKey,
                    onVariant = onVariant,
                    onFeedback = onFeedback,
                    modifier = Modifier.weight(k.weight),
                    enterLabel = enterLabel
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardKeyView(
    keyData: KyrgyzKey,
    fontSize: Float,
    shiftActive: Boolean,
    shiftLocked: Boolean,
    onKey: (KyrgyzKey) -> Unit,
    onVariant: (String) -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    enterLabel: String? = null
) {
    val isShift = keyData.function == FunctionType.SHIFT
    val isDigital = keyData.type == KeyType.DIGITAL
    val hasVariants = keyData.variants.isNotEmpty()

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // 按下瞬间给一次反馈（比松手时更跟手）
    LaunchedEffect(pressed) { if (pressed) onFeedback() }

    var showBubble by remember { mutableStateOf(false) }

    val baseBg = when {
        isDigital -> MaterialTheme.colorScheme.tertiaryContainer
        isShift && shiftLocked -> MaterialTheme.colorScheme.primary
        isShift && shiftActive -> MaterialTheme.colorScheme.primary.copy(alpha = KeyboardTokens.ShiftActiveAlpha)
        keyData.type == KeyType.FUNCTION -> MaterialTheme.colorScheme.secondaryContainer
        keyData.type == KeyType.SYMBOL -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val fg = when {
        isDigital -> MaterialTheme.colorScheme.onTertiaryContainer
        isShift && (shiftActive || shiftLocked) -> MaterialTheme.colorScheme.onPrimary
        keyData.type == KeyType.FUNCTION -> MaterialTheme.colorScheme.onSecondaryContainer
        keyData.type == KeyType.SYMBOL -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    // 按下高亮：叠加主色调，并轻微放大
    val bg = if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = KeyboardTokens.PressedBgAlpha) else baseBg
    val contentColor = if (pressed) MaterialTheme.colorScheme.onPrimary else fg
    val fontWeight = if (isShift && shiftLocked) FontWeight.Bold else FontWeight.Normal

    val clickModifier = if (hasVariants) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onKey(keyData) },
            onLongClick = {
                onFeedback()
                showBubble = true
            }
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onKey(keyData) }
        )
    }

    Box(
        modifier = modifier
            .scale(if (pressed) 1.06f else 1f)
            .clip(RoundedCornerShape(KeyboardTokens.KeyCorner))
            .background(bg)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (isDigital && hasVariants) {
            // 九宫格键：数字右上角 + 字母居中（存档布局用）
            Text(
                text = keyData.label,
                color = contentColor.copy(alpha = KeyboardTokens.NineGridSecondaryAlpha),
                fontSize = KeyboardTokens.NineGridDigitSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                keyData.variants.take(4).forEachIndexed { i, char ->
                    Text(
                        text = char,
                        color = contentColor.copy(alpha = if (i == 0) 1.0f else KeyboardTokens.NineGridSecondaryAlpha),
                        fontSize = if (i == 0) KeyboardTokens.NineGridPrimarySize else KeyboardTokens.NineGridSecondarySize,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 字号规则：数字键略大于字母键；功能键统一固定字号（不随用户字号缩放，保证标签风格一致）；字母/符号键跟随用户字号
            val isFunctionKey = keyData.type == KeyType.FUNCTION
            val labelSize = when {
                isDigital -> fontSize + KeyboardTokens.DigitalLabelBoost
                isFunctionKey -> KeyboardTokens.FunctionLabelSize
                else -> fontSize
            }
            // 功能键统一 Medium 字重；Shift 锁定时加粗
            val labelWeight = when {
                isShift && shiftLocked -> FontWeight.Bold
                isFunctionKey -> FontWeight.Medium
                else -> fontWeight
            }
            val displayLabel =
                if (keyData.function == FunctionType.NEWLINE && enterLabel != null) {
                    enterLabel
                } else {
                    keyData.label
                }
            Text(
                text = displayLabel,
                color = contentColor,
                fontSize = labelSize.sp,
                fontWeight = labelWeight,
                textAlign = TextAlign.Center
            )
        }

        if (showBubble && hasVariants) {
            VariantBubble(
                variants = keyData.variants,
                onPick = {
                    onVariant(it)
                    showBubble = false
                },
                onDismiss = { showBubble = false }
            )
        }
    }
}

/**
 * 长按变体气泡：浮在键上方，横向罗列全部变体，点选即上屏。
 */
@Composable
private fun VariantBubble(
    variants: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val yOffset = with(LocalDensity.current) { KeyboardTokens.BubbleOffsetY.roundToPx() }
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, yOffset),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(KeyboardTokens.BubbleCorner),
            shadowElevation = 6.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(KeyboardTokens.KeyGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                variants.forEach { variant ->
                    Box(
                        modifier = Modifier
                            .height(KeyboardTokens.BubbleKeyHeight)
                            .clip(RoundedCornerShape(KeyboardTokens.BubbleKeyCorner))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onPick(variant) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = variant,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = KeyboardTokens.BubbleKeySize,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
