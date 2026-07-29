package com.example.kyrgyzkeyboard.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 键盘视觉设计 token（P3 视觉统一）。
 *
 * 集中管理圆角 / 间距 / 尺寸 / 字号 / 透明度，供 IME 服务、预览页、共享键盘组件
 * 三处复用，避免散落的 magic number 造成视觉漂移。改一处即可全局生效。
 */
object KeyboardTokens {

    // ---- 圆角 ----
    val KeyCorner = 8.dp                 // 普通按键圆角
    val CandidateBarCorner = 6.dp        // 候选栏容器圆角
    val CandidateChipCorner = 6.dp       // 候选词点选区圆角
    val BubbleCorner = 12.dp             // 长按变体气泡外框圆角
    val BubbleKeyCorner = 8.dp           // 气泡内单个变体键圆角

    // ---- 间距 ----
    val KeyGap = 4.dp                    // 键与键横向间距
    val RowGap = 4.dp                    // 行与行纵向间距
    val BoardPadding = 4.dp              // 键盘整体外边距

    // ---- 尺寸 ----
    val RowHeight = 52.dp                // 统一键行高度（IME 与预览一致）
    val CandidateBarHeight = 44.dp       // 候选栏高度
    val BubbleKeyHeight = 44.dp          // 气泡内变体键高度
    val BubbleOffsetY = (-58).dp         // 气泡相对按键的上浮偏移

    // ---- 字号（sp 的裸浮点，供 .sp 使用）----
    const val DigitalLabelBoost = 2f     // 数字键字号相对基准字号的增量
    const val FunctionLabelSize = 16f    // 功能键标签统一固定字号（不随用户字号缩放）
    val NineGridDigitSize = 11.sp        // 九宫格右上角数字
    val NineGridPrimarySize = 16.sp      // 九宫格首选字母
    val NineGridSecondarySize = 13.sp    // 九宫格次选字母
    val CandidateComposingSize = 14.sp   // 候选栏拼音串
    val CandidateWordSize = 20.sp        // 候选词
    val CandidatePlaceholderSize = 13.sp // 候选栏占位提示
    val BubbleKeySize = 20.sp            // 气泡内变体字符

    // ---- 透明度 ----
    const val ShiftActiveAlpha = 0.7f        // Shift 临时激活背景
    const val PressedBgAlpha = 0.85f         // 按下高亮背景
    const val NineGridSecondaryAlpha = 0.6f  // 九宫格次选字母 / 数字角标
    const val CandidateBarBgAlpha = 0.3f     // 候选栏背景
    const val PlaceholderAlpha = 0.6f        // 占位文案
}