// Added for tri-language support: 柯语 / 英文 / 中文拼音
package com.example.kyrgyzkeyboard.keyboard

/** 输入语言。*/
enum class Language {
    KYRGYZ,   // 柯尔克孜语（阿拉伯字母）
    ENGLISH,  // 英文
    PINYIN;   // 中文拼音

    fun next(): Language = when (this) {
        KYRGYZ -> ENGLISH
        ENGLISH -> PINYIN
        PINYIN -> KYRGYZ
    }

    /** 显示在语言键上的短标签。*/
    val label: String
        get() = when (this) {
            KYRGYZ -> "قىر"
            ENGLISH -> "EN"
            PINYIN -> "中"
        }
}