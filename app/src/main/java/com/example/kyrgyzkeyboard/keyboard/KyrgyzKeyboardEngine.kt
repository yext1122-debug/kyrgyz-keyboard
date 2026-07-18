// Modified for tri-language support: 柯语 / 英文 / 中文拼音
package com.example.kyrgyzkeyboard.keyboard

/**
 * 键盘引擎：管理当前语言、布局、拼音候选状态。
 *
 * Engine 本身不持有 View / Context，纯逻辑；通过 [Listener] 把动作回调给宿主（IME Service）。
 */
class KyrgyzKeyboardEngine(private val listener: Listener) {

    interface Listener {
        fun onKeyPress(code: String)
        fun onKeyAction(action: FunctionType)
        /** 拼音正在编辑串更新（未 commit）。空串表示清除。*/
        fun onComposingChanged(composing: String) {}
        /** 提交拼音候选：宿主应用此文本替换掉当前 composing 区域，不产生残留。*/
        fun onCommitCandidate(text: String) {}
    }

    var currentLanguage: Language = Language.KYRGYZ
        private set

    /** 柯语/英语内的子布局（字母 vs 符号）。*/
    var currentLayout: LayoutType = LayoutType.DEFAULT
        private set

    /** 英文是否大写。*/
    var englishShift: Boolean = false
        private set

    /** 拼音编辑串。*/
    private val composingBuilder = StringBuilder()
    val composing: String get() = composingBuilder.toString()

    /** 当前候选列表。*/
    var candidates: List<String> = emptyList()
        private set

    fun getRows(): List<KeyboardRow> = when (currentLanguage) {
        Language.KYRGYZ -> when (currentLayout) {
            LayoutType.DEFAULT -> KyrgyzKeyboardLayouts.DefaultLayout
            LayoutType.SYMBOL -> KyrgyzKeyboardLayouts.SymbolLayout
        }
        Language.ENGLISH -> when (currentLayout) {
            LayoutType.DEFAULT -> EnglishKeyboardLayouts.letterLayout(englishShift)
            LayoutType.SYMBOL -> KyrgyzKeyboardLayouts.SymbolLayout
        }
        Language.PINYIN -> when (currentLayout) {
            LayoutType.DEFAULT -> PinyinKeyboardLayouts.layout
            LayoutType.SYMBOL -> KyrgyzKeyboardLayouts.SymbolLayout
        }
    }

    fun switchLayout() {
        currentLayout = when (currentLayout) {
            LayoutType.DEFAULT -> LayoutType.SYMBOL
            LayoutType.SYMBOL -> LayoutType.DEFAULT
        }
    }

    /** 循环切换语言。切换时清空拼音编辑串。*/
    fun switchLanguage() {
        clearComposing()
        currentLanguage = currentLanguage.next()
        currentLayout = LayoutType.DEFAULT
        englishShift = false
    }

    /** 处理按键。返回 true 表示已消费。*/
    fun handleKey(key: KyrgyzKey): Boolean {
        return when (key.type) {
            KeyType.LETTER -> handleLetter(key)
            KeyType.SYMBOL -> {
                commitComposing()
                if (key.code.isNotEmpty()) listener.onKeyPress(key.code)
                true
            }
            KeyType.FUNCTION -> {
                val fn = key.function ?: return false
                handleFunction(fn)
                true
            }
        }
    }

    private fun handleLetter(key: KyrgyzKey): Boolean {
        val code = key.code
        if (currentLanguage == Language.PINYIN && code.length == 1 && code[0] in 'a'..'z') {
            composingBuilder.append(code)
            updateCandidates()
            listener.onComposingChanged(composing)
            return true
        }
        if (code.isNotEmpty()) {
            val out = if (currentLanguage == Language.ENGLISH && englishShift) {
                code.uppercase().also { englishShift = false }
            } else code
            listener.onKeyPress(out)
        }
        return true
    }

    private fun handleFunction(fn: FunctionType) {
        when (fn) {
            FunctionType.SWITCH_LAYOUT -> {
                commitComposing()
                switchLayout()
            }
            FunctionType.SWITCH_LANGUAGE -> {
                switchLanguage()
            }
            FunctionType.SHIFT -> {
                if (currentLanguage == Language.ENGLISH) {
                    englishShift = !englishShift
                }
            }
            FunctionType.BACKSPACE -> {
                if (currentLanguage == Language.PINYIN && composingBuilder.isNotEmpty()) {
                    composingBuilder.deleteCharAt(composingBuilder.length - 1)
                    updateCandidates()
                    listener.onComposingChanged(composing)
                    return
                }
                listener.onKeyAction(FunctionType.BACKSPACE)
            }
            FunctionType.SPACE -> {
                if (currentLanguage == Language.PINYIN && candidates.isNotEmpty()) {
                    selectCandidate(0)
                    return
                }
                if (currentLanguage == Language.PINYIN && composingBuilder.isNotEmpty()) {
                    // 无候选时空格直接上屏拼音串本身（替换 composing）
                    val text = composing
                    composingBuilder.clear()
                    candidates = emptyList()
                    listener.onCommitCandidate(text)
                    return
                }
                listener.onKeyAction(FunctionType.SPACE)
            }
            FunctionType.NEWLINE -> {
                commitComposing()
                listener.onKeyAction(FunctionType.NEWLINE)
            }
            FunctionType.DONE -> {
                clearComposing()
                listener.onKeyAction(FunctionType.DONE)
            }
        }
    }

    /** 从候选栏点选第 [index] 个候选（0 起）。*/
    fun selectCandidate(index: Int) {
        if (index !in candidates.indices) return
        val chosen = candidates[index]
        composingBuilder.clear()
        candidates = emptyList()
        listener.onCommitCandidate(chosen)
    }

    /** 长按变体：直接上屏，若在拼音模式则先 commit 现有 composing。*/
    fun handleVariant(variant: String): Boolean {
        if (variant.isEmpty()) return false
        commitComposing()
        listener.onKeyPress(variant)
        return true
    }

    private fun updateCandidates() {
        candidates = if (composingBuilder.isEmpty()) emptyList()
        else PinyinDictionary.query(composingBuilder.toString())
    }

    private fun commitComposing() {
        if (composingBuilder.isNotEmpty()) {
            val text = if (candidates.isNotEmpty()) candidates[0] else composing
            composingBuilder.clear()
            candidates = emptyList()
            listener.onCommitCandidate(text)
        }
    }

    private fun clearComposing() {
        if (composingBuilder.isNotEmpty()) {
            composingBuilder.clear()
            candidates = emptyList()
            listener.onComposingChanged("")
        }
    }
}