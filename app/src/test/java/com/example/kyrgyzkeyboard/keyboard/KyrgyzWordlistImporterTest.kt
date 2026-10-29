package com.example.kyrgyzkeyboard.keyboard

import java.io.BufferedReader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 词表解析器测试。
 *
 * 锁定的行为：
 * - `词<TAB>频次<TAB>释义` 三列解析
 * - 无制表符时按首个空白切出词形
 * - `#` 注释与空行跳过
 * - 非法频次按 0
 */
class KyrgyzWordlistImporterTest {

    private fun parse(text: String): List<KyrgyzWordlistImporter.Entry> =
        KyrgyzWordlistImporter.parse(BufferedReader(StringReader(text)))

    @Test
    fun `制表符三列正常解析`() {
        val e = KyrgyzWordlistImporter.parseLine("алма\t5\t苹果")
        assertEquals("алма", e?.word)
        assertEquals(5, e?.frequency)
        assertEquals("苹果", e?.meaning)
    }

    @Test
    fun `无制表符时按空白切出词形与频次`() {
        val e = KyrgyzWordlistImporter.parseLine("алма 5")
        assertEquals("алма", e?.word)
        assertEquals(5, e?.frequency)
        assertNull("无第三列时释义应为 null", e?.meaning)
    }

    @Test
    fun `注释与空行被跳过`() {
        assertNull(KyrgyzWordlistImporter.parseLine("# 这是注释"))
        assertNull(KyrgyzWordlistImporter.parseLine(""))
        assertNull(KyrgyzWordlistImporter.parseLine("    "))

        val list = parse(
            """
            # 种子词表
            алма	5	苹果

            алмурт	3	梨
            """.trimIndent()
        )
        assertEquals("只应解析出 2 条", 2, list.size)
        assertEquals(listOf("алма", "алмурт"), list.map { it.word })
    }

    @Test
    fun `非法频次按 0 处理`() {
        assertEquals(0, KyrgyzWordlistImporter.parseLine("алма\tabc\t苹果")?.frequency)
        assertEquals(0, KyrgyzWordlistImporter.parseLine("алма\t\t苹果")?.frequency)
        assertEquals("负频次应被夹到 0", 0, KyrgyzWordlistImporter.parseLine("алма\t-3")?.frequency)
    }

    @Test
    fun `缺省频次为 0`() {
        val e = KyrgyzWordlistImporter.parseLine("алма")
        assertEquals("алма", e?.word)
        assertEquals(0, e?.frequency)
        assertNull(e?.meaning)
    }

    @Test
    fun `无制表符的三列行频次退化为 0（已知限制）`() {
        // 无制表符时 limit=2 会把 "5 苹果" 整体当作频次字段，无法解析成整数 → 0。
        // 词表应使用制表符分隔；此断言用于锁定当前行为，避免无声变化。
        val e = KyrgyzWordlistImporter.parseLine("алма 5 苹果")
        assertEquals("алма", e?.word)
        assertEquals(0, e?.frequency)
        assertNull(e?.meaning)
    }

    @Test
    fun `全空行文本返回空列表`() {
        assertTrue(parse("\n\n   \n").isEmpty())
    }
}
