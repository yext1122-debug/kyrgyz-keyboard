// Added for tri-language support
package com.example.kyrgyzkeyboard.keyboard

/** 中文拼音键盘布局：小写字母 + 语言切换键。*/
object PinyinKeyboardLayouts {

    private val row1 = listOf("q","w","e","r","t","y","u","i","o","p")
    private val row2 = listOf("a","s","d","f","g","h","j","k","l")
    private val row3 = listOf("z","x","c","v","b","n","m")

    private fun letters(chars: List<String>): KeyboardRow =
        KeyboardRow(chars.map { KyrgyzKey.letter(it) })

    val layout: List<KeyboardRow> = listOf(
        letters(row1),
        letters(row2),
        KeyboardRow(listOf(KyrgyzKey.shift()) + row3.map { KyrgyzKey.letter(it) } + listOf(KyrgyzKey.backspace())),
        KeyboardRow(listOf(
            KyrgyzKey.switchLayout("123"),
            KyrgyzKey.switchLanguage(Language.PINYIN.label),
            KyrgyzKey.symbol("，"),
            KyrgyzKey.space("选定"),
            KyrgyzKey.symbol("。"),
            KyrgyzKey.newline()
        ))
    )
}