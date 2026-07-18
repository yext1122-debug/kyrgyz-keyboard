// Added for tri-language support
package com.example.kyrgyzkeyboard.keyboard

/** 英文 QWERTY 键盘布局（含 Shift 大小写、符号页）。*/
object EnglishKeyboardLayouts {

    private val row1 = listOf("q","w","e","r","t","y","u","i","o","p")
    private val row2 = listOf("a","s","d","f","g","h","j","k","l")
    private val row3 = listOf("z","x","c","v","b","n","m")

    private fun letterRow(chars: List<String>, upper: Boolean): KeyboardRow =
        KeyboardRow(chars.map {
            val ch = if (upper) it.uppercase() else it
            KyrgyzKey.letter(ch)
        })

    fun letterLayout(upper: Boolean): List<KeyboardRow> = listOf(
        letterRow(row1, upper),
        letterRow(row2, upper),
        KeyboardRow(listOf(KyrgyzKey.shift()) + row3.map {
            val ch = if (upper) it.uppercase() else it
            KyrgyzKey.letter(ch)
        } + listOf(KyrgyzKey.backspace())),
        KeyboardRow(listOf(
            KyrgyzKey.switchLayout("123"),
            KyrgyzKey.switchLanguage(Language.ENGLISH.label),
            KyrgyzKey.symbol(","),
            KyrgyzKey.space(),
            KyrgyzKey.symbol("."),
            KyrgyzKey.newline()
        ))
    )
}