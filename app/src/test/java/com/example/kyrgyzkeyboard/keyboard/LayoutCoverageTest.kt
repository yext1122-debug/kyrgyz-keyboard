package com.example.kyrgyzkeyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T01 验收：布局覆盖测试。
 *
 * 依据 GB/T 31917-2015 附录 A（30 字母）与表 2（键位配置）。
 */
class LayoutCoverageTest {

    private val layers: List<Pair<String, List<KeyboardRow>>> = listOf(
        "DefaultLayout" to KyrgyzKeyboardLayouts.DefaultLayout,
        "ShiftLayout" to KyrgyzKeyboardLayouts.ShiftLayout
    )

    /**
     * 表 2 第 2 层允许出现的非 30 字母字符：ف گ ح。
     * 它们由标准分配在第 2 层，但不属于附录 A 的 30 字母。
     */
    private val layer2Extras: Set<String> = setOf("\u0641", "\u06AF", "\u062D")

    /** 两层布局中所有可提交的字符（含长按变体）。 */
    private fun reachableChars(): Set<String> =
        layers.flatMap { it.second }
            .flatMap { it.keys }
            .flatMap { key -> listOf(key.code) + key.variants }
            .filter { it.isNotEmpty() }
            .toSet()

    @Test
    fun `30 个标准字母全部可输入`() {
        val reachable = reachableChars()
        val missing = KyrgyzAlphabet.LETTERS.filterNot { it in reachable }
        assertTrue("布局缺少标准字母: $missing", missing.isEmpty())
    }

    @Test
    fun `字母键不含标准外字符`() {
        // 允许集合 = 30 字母 ∪ {ھ(C01)} ∪ 表 2 第 2 层字符
        val allowed = KyrgyzAlphabet.LETTERS.toSet() + "\u06BE" + layer2Extras
        val offenders = layers.flatMap { (name, rows) ->
            rows.flatMap { it.keys }
                .filter { it.type == KeyType.LETTER }
                .filterNot { it.code in allowed }
                .map { "$name:${it.code}" }
        }
        assertTrue("出现标准外字母键: $offenders", offenders.isEmpty())
    }

    @Test
    fun `ھ 位于 C01 键`() {
        val c01 = KyrgyzKeyboardLayouts.DefaultLayout[0].keys[1]
        assertEquals("C01 应为 ھ(U+06BE)", "\u06BE", c01.code)
        assertEquals(KeyType.LETTER, c01.type)
    }

    @Test
    fun `ج 与 ۅ 可输入`() {
        val reachable = reachableChars()
        assertTrue("ج(062C) 不可输入", "\u062C" in reachable)
        assertTrue("ۅ(06C5) 不可输入", "\u06C5" in reachable)
    }

    @Test
    fun `表 2 第 2 层键位逐键一致`() {
        val shiftC = KyrgyzKeyboardLayouts.ShiftLayout[0].keys
        val shiftB = KyrgyzKeyboardLayouts.ShiftLayout[2].keys

        assertEquals("C04 第 2 层应为 ف", "\u0641", shiftC[4].code)
        assertEquals("C05 第 2 层应为 گ", "\u06AF", shiftC[5].code)
        assertEquals("C06 第 2 层应为 ح", "\u062D", shiftC[6].code)
        assertEquals("C11 第 2 层应为 «", "\u00AB", shiftC[11].code)
        assertEquals("B10 第 2 层应为 ؟", "\u061F", shiftB[10].code)
    }

    @Test
    fun `第 1 层 B 行与表 2 逐键一致`() {
        val rowB = KyrgyzKeyboardLayouts.DefaultLayout[2].keys
        assertEquals("B 行键数应为 12（Shift + B01–B10 + 退格）", 12, rowB.size)

        assertEquals(FunctionType.SHIFT, rowB[0].function)

        val expectedCodes = listOf(
            "\u0632", // B01 ز
            "\u0634", // B02 ش
            "\u0639", // B03 ع
            "\u06C9", // B04 ۉ
            "\u0628", // B05 ب
            "\u0646", // B06 ن
            "\u0645", // B07 م
            "\u060C", // B08 ،
            "\u002E", // B09 .
            "\u0626"  // B10 ئ
        )
        expectedCodes.forEachIndexed { i, code ->
            assertEquals("B${(i + 1).toString().padStart(2, '0')} 码位不符", code, rowB[i + 1].code)
        }

        assertEquals(FunctionType.BACKSPACE, rowB[11].function)
    }
}
