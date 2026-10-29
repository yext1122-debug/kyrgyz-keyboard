package com.example.kyrgyzkeyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 柯语前缀索引（Trie）测试。
 *
 * 锁定的行为：
 * - put 后可按前缀命中
 * - 排序规则：频次降序 → 词长升序 → 字典序
 * - 排除与前缀完全相同的词
 * - increment 会改变排序
 * - 空前缀 / 非正 limit 返回空
 */
class KyrgyzPrefixIndexTest {

    @Test
    fun `put 后可按前缀命中`() {
        val index = KyrgyzPrefixIndex()
        index.put("алма", 5)
        index.put("алмаз", 3)

        val result = index.query("ал", 10)
        assertTrue("应命中 алма，实际: $result", result.contains("алма"))
        assertTrue("应命中 алмаз，实际: $result", result.contains("алмаз"))
        assertEquals("size 应为 2", 2, index.size)
    }

    @Test
    fun `频次降序优先`() {
        val index = KyrgyzPrefixIndex()
        index.put("алма", 9)
        index.put("алмаз", 3)

        assertEquals(listOf("алма", "алмаз"), index.query("ал", 10))
    }

    @Test
    fun `同频次时短词在前`() {
        val index = KyrgyzPrefixIndex()
        index.put("алмаз", 5)
        index.put("алма", 5)
        index.put("алм", 5)

        // 词长升序：алм(3) → алма(4) → алмаз(5)
        assertEquals(listOf("алм", "алма", "алмаз"), index.query("ал", 10))
    }

    @Test
    fun `排除与前缀完全相同的词`() {
        val index = KyrgyzPrefixIndex()
        index.put("ал", 100)
        index.put("алма", 5)

        val result = index.query("ал", 10)
        assertEquals("只应返回 алма，前缀本身要被排除", listOf("алма"), result)
    }

    @Test
    fun `increment 后排序变化`() {
        val index = KyrgyzPrefixIndex()
        index.put("алма", 1)
        index.put("алмаз", 2)
        assertEquals(listOf("алмаз", "алма"), index.query("ал", 10))

        val newFreq = index.increment("алма", 5)
        assertEquals("increment 应返回累加后的频次", 6, newFreq)
        assertEquals(listOf("алма", "алмаз"), index.query("ал", 10))
    }

    @Test
    fun `空前缀与非正 limit 返回空`() {
        val index = KyrgyzPrefixIndex()
        index.put("алма", 5)

        assertTrue(index.query("", 10).isEmpty())
        assertTrue("全空白前缀应被 trim 成空", index.query("   ", 10).isEmpty())
        assertTrue("limit <= 0 应返回空", index.query("ал", 0).isEmpty())
    }

    @Test
    fun `limit 截断生效`() {
        val index = KyrgyzPrefixIndex()
        index.put("алма", 9)
        index.put("алмаз", 8)
        index.put("алм", 7)

        val result = index.query("ал", 2)
        assertEquals(2, result.size)
        assertEquals(listOf("алма", "алмаз"), result)
    }

    @Test
    fun `空白词不会入索引`() {
        val index = KyrgyzPrefixIndex()
        index.put("", 5)
        index.put("   ", 5)

        assertEquals(0, index.size)
        assertTrue(index.query(" ", 10).isEmpty())
    }
}
