package com.example.kyrgyzkeyboard.keyboard

import com.example.kyrgyzkeyboard.utils.BidiControls
import com.example.kyrgyzkeyboard.utils.KyrgyzCharacterUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 布局与字符集测试。
 *
 * 覆盖：
 * - T01 验收：30 字母全覆盖、C01 = ھ、第 2 层与 B 行逐键一致、ج/ۅ 可输入
 * - T02 验收：字符集单一真源（映射派生、归一化不越界、双向字符集包含字母表）
 */
class LayoutCoverageTest {

    private val layers: List<Pair<String, List<KeyboardRow>>> = listOf(
        "DefaultLayout" to KyrgyzKeyboardLayouts.DefaultLayout,
        "ShiftLayout" to KyrgyzKeyboardLayouts.ShiftLayout
    )

    /** 两层布局中所有可提交的字符（含长按变体）。 */
    private fun reachableChars(): Set<String> =
        layers.flatMap { it.second }
            .flatMap { it.keys }
            .flatMap { key -> listOf(key.code) + key.variants }
            .filter { it.isNotEmpty() }
            .toSet()

    // ------------------------------------------------------------
    // T01：键位覆盖
    // ------------------------------------------------------------

    @Test
    fun `30 个标准字母全部可输入`() {
        val reachable = reachableChars()
        val missing = KyrgyzAlphabet.LETTERS.filterNot { it in reachable }
        assertTrue("布局缺少标准字母: $missing", missing.isEmpty())
    }

    @Test
    fun `字母键不含标准外字符`() {
        // 允许集合 = 30 字母 ∪ {ھ(C01)}
        val allowed = KyrgyzAlphabet.KEYBOARD_LETTERS
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
        assertEquals("C01 应为 ھ", KyrgyzAlphabet.HEH_DOACHASHMEE, c01.code)
        assertEquals(KeyType.LETTER, c01.type)
    }

    @Test
    fun `ج 与 ۅ 可输入`() {
        val reachable = reachableChars()
        assertTrue("ج(062C) 不可输入", KyrgyzAlphabet.JEEM in reachable)
        assertTrue("ۅ(06C5) 不可输入", KyrgyzAlphabet.OE in reachable)
    }

    @Test
    fun `表 2 第 2 层键位逐键一致`() {
        val shiftC = KyrgyzKeyboardLayouts.ShiftLayout[0].keys
        val shiftB = KyrgyzKeyboardLayouts.ShiftLayout[2].keys

        assertEquals("C04 第 2 层应为 ف", KyrgyzAlphabet.FEH, shiftC[4].code)
        assertEquals("C05 第 2 层应为 گ", KyrgyzAlphabet.GAF, shiftC[5].code)
        assertEquals("C06 第 2 层应为 ح", KyrgyzAlphabet.HAH, shiftC[6].code)
        assertEquals("C11 第 2 层应为 «", KyrgyzAlphabet.GUILLEMET_LEFT, shiftC[11].code)
        assertEquals("B10 第 2 层应为 ؟", KyrgyzAlphabet.QUESTION, shiftB[10].code)
    }

    @Test
    fun `第 1 层 B 行与表 2 逐键一致`() {
        val rowB = KyrgyzKeyboardLayouts.DefaultLayout[2].keys
        assertEquals("B 行键数应为 12（Shift + B01–B10 + 退格）", 12, rowB.size)

        assertEquals(FunctionType.SHIFT, rowB[0].function)

        val expectedCodes = listOf(
            KyrgyzAlphabet.ZAIN,   // B01
            KyrgyzAlphabet.SHEEN,  // B02
            KyrgyzAlphabet.AIN,    // B03
            KyrgyzAlphabet.UE,     // B04
            KyrgyzAlphabet.BE,     // B05
            KyrgyzAlphabet.NUN,    // B06
            KyrgyzAlphabet.MEEM,   // B07
            KyrgyzAlphabet.COMMA,  // B08
            KyrgyzAlphabet.PERIOD, // B09
            KyrgyzAlphabet.HAMZA   // B10
        )
        expectedCodes.forEachIndexed { i, code ->
            assertEquals("B${(i + 1).toString().padStart(2, '0')} 码位不符", code, rowB[i + 1].code)
        }

        assertEquals(FunctionType.BACKSPACE, rowB[11].function)
    }

    // ------------------------------------------------------------
    // T02：字符集单一真源
    // ------------------------------------------------------------

    @Test
    fun `布局由 LAYER1 派生而非另行硬编码`() {
        val rowKeys = listOf("asdfghjkl;'", "qwertyuiop", "zxcvbnm,./")

        rowKeys.forEachIndexed { rowIndex, keySeq ->
            val keys = KyrgyzKeyboardLayouts.DefaultLayout[rowIndex].keys
            val offset = if (rowIndex == 2) 1 else if (rowIndex == 0) 1 else 0
            val actual = keys.subList(offset, offset + keySeq.length).map { it.code }
            val expected = keySeq.map { KyrgyzAlphabet.LAYER1.getValue(it) }
            assertEquals("第 ${rowIndex + 1} 行未按 LAYER1 生成", expected, actual)
        }
    }

    @Test
    fun `字符工具的字母集与 KyrgyzAlphabet 完全一致`() {
        assertEquals(
            "ALL_LETTERS 与 LETTERS 不一致",
            KyrgyzAlphabet.LETTERS.toSet(),
            KyrgyzCharacterUtils.ALL_LETTERS
        )
        assertEquals("元音数应为 8", 8, KyrgyzCharacterUtils.VOWELS.size)
        assertEquals("辅音数应为 22", 22, KyrgyzCharacterUtils.CONSONANTS.size)
        assertEquals("字母表应为 30 个", 30, KyrgyzAlphabet.LETTERS.size)
    }

    @Test
    fun `归一化输出不含标准集外字符`() {
        // 含 tatweel、波斯 ي、多余空白
        val raw = KyrgyzAlphabet.BE + KyrgyzAlphabet.TATWEEL + KyrgyzAlphabet.FARSI_YEH +
            "  " + KyrgyzAlphabet.NUN + KyrgyzAlphabet.ALEF
        val out = KyrgyzCharacterUtils.normalizeArabicText(raw)

        assertTrue("归一化后仍含 tatweel", KyrgyzAlphabet.TATWEEL !in out)
        assertTrue("归一化后仍含波斯 ي", KyrgyzAlphabet.FARSI_YEH !in out)

        val illegal = out.filterNot { it.isWhitespace() || KyrgyzCharacterUtils.isKyrgyzLetter(it) }
        assertTrue("归一化输出含非标准字符: $illegal", illegal.isEmpty())
    }

    @Test
    fun `双向字符集覆盖全部标准字母`() {
        val missing = KyrgyzAlphabet.LETTERS
            .map { it[0] }
            .filterNot { it in BidiControls.KYRGYZ_CHARS }
        assertTrue("BidiControls.KYRGYZ_CHARS 缺少: $missing", missing.isEmpty())
    }
}
