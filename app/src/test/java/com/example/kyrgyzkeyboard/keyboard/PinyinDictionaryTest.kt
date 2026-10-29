package com.example.kyrgyzkeyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 拼音字典测试。
 *
 * 锁定的行为：
 * - 完整音节精确命中，且首选字符合常用度排序
 * - 不完整输入走前缀匹配
 * - 多音节串当前不支持（返回空），等 T11 补齐后改断言
 * - 大小写不敏感
 */
class PinyinDictionaryTest {

    @Test
    fun `完整音节命中且首候选为常用字`() {
        val result = PinyinDictionary.query("ni")
        assertTrue("ni 应命中候选，实际: $result", result.isNotEmpty())
        assertEquals("ni 的首候选应为「你」", "你", result.first())
        assertTrue("应包含「泥」", result.contains("泥"))
    }

    @Test
    fun `前缀匹配`() {
        val result = PinyinDictionary.query("n")
        assertTrue("n 应有前缀候选", result.isNotEmpty())
        assertTrue("n 的前缀候选应包含「你」", result.contains("你"))
        assertTrue("n 的前缀候选应包含「南」", result.contains("南"))
    }

    @Test
    fun `多音节串当前返回空（待 T11）`() {
        // 当前只支持单音节；T11 补齐多音节/词级候选后，本断言应改为非空。
        assertTrue("nihao 目前应返回空", PinyinDictionary.query("nihao").isEmpty())
    }

    @Test
    fun `大小写不敏感`() {
        assertEquals(PinyinDictionary.query("ni"), PinyinDictionary.query("NI"))
        assertEquals(PinyinDictionary.query("zhong"), PinyinDictionary.query("Zhong"))
    }

    @Test
    fun `limit 生效`() {
        val result = PinyinDictionary.query("ni", limit = 2)
        assertEquals(2, result.size)
        assertEquals(listOf("你", "尼"), result)
    }

    @Test
    fun `空输入返回空`() {
        assertTrue(PinyinDictionary.query("").isEmpty())
    }
}
