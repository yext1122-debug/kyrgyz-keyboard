// Modified for tri-language support: 柯语 / 英文 / 中文拼音
// Updated: v1.2.4 修复 1 键和 0 键无法输出数字的问题
package com.example.kyrgyzkeyboard.keyboard

import com.example.kyrgyzkeyboard.utils.BidiControls

class KyrgyzKeyboardEngine(private val listener: Listener) {

    interface Listener {
        fun onKeyPress(code: String)
        fun onKeyAction(action: FunctionType)
        fun onComposingChanged(composing: String) {}
        fun onCommitCandidate(text: String) {}
        fun onDeleteBeforeCursor(count: Int) {}
    }

    var currentLanguage: Language = Language.KYRGYZ
        private set

    var currentLayout: LayoutType = LayoutType.DEFAULT
        private set

    var englishShift: Boolean = false
        private set

    var kyrgyzShift: Boolean = false
        private set

    var shiftLocked: Boolean = false
        private set

    private val composingBuilder = StringBuilder()
    val composing: String get() = composingBuilder.toString()

    var candidates: List<String> = emptyList()
        private set

    /** 柯语联想开关（默认关，纯本地）。关闭时柯语走原有逐字母直接上屏路径。*/
    var kyrgyzAssociationEnabled: Boolean = false

    /** 柯语词库提供者，由 Service 注入；为 null 时联想不生效。*/
    var kyrgyzWordProvider: KyrgyzWordProvider? = null

    private var lastKey: Char? = null
    private var keyPressCount: Int = 0
    private var lastPressTime: Long = 0

    private var lastChar: Char? = null

    // 数字键盘连按时，上一次已提交到编辑器的字符数（含双向控制符），用于替换而非追加
    private var lastEmittedLength: Int = 0

    fun getRows(): List<KeyboardRow> = when (currentLanguage) {
        Language.KYRGYZ -> when (currentLayout) {
            LayoutType.DEFAULT -> KyrgyzKeyboardLayouts.forType(LayoutType.DEFAULT, kyrgyzShift)
            LayoutType.DIGITAL -> KyrgyzKeyboardLayouts.forType(LayoutType.DIGITAL)
            LayoutType.SYMBOL -> KyrgyzKeyboardLayouts.forType(LayoutType.SYMBOL)
        }
        Language.ENGLISH -> when (currentLayout) {
            LayoutType.DEFAULT -> EnglishKeyboardLayouts.letterLayout(englishShift)
            LayoutType.SYMBOL -> KyrgyzKeyboardLayouts.forType(LayoutType.SYMBOL)
            else -> EnglishKeyboardLayouts.letterLayout(englishShift)
        }
        Language.PINYIN -> when (currentLayout) {
            LayoutType.DEFAULT -> PinyinKeyboardLayouts.layout
            LayoutType.SYMBOL -> KyrgyzKeyboardLayouts.forType(LayoutType.SYMBOL)
            else -> PinyinKeyboardLayouts.layout
        }
    }

    fun switchLayout() {
        currentLayout = when (currentLayout) {
            LayoutType.DEFAULT -> LayoutType.DIGITAL
            LayoutType.DIGITAL -> LayoutType.SYMBOL
            LayoutType.SYMBOL -> LayoutType.DEFAULT
        }
        kyrgyzShift = false
        shiftLocked = false
        clearComposing()
        resetKeyPressCounter()
    }

    fun switchToDigital() {
        if (currentLayout != LayoutType.DIGITAL) {
            currentLayout = LayoutType.DIGITAL
            kyrgyzShift = false
            shiftLocked = false
            clearComposing()
            resetKeyPressCounter()
        } else {
            currentLayout = LayoutType.DEFAULT
        }
    }

    fun switchLanguage() {
        clearComposing()
        resetKeyPressCounter()
        currentLanguage = currentLanguage.next()
        currentLayout = LayoutType.DEFAULT
        englishShift = false
        kyrgyzShift = false
        shiftLocked = false
    }

    fun handleKey(key: KyrgyzKey): Boolean {
        return when (key.type) {
            KeyType.LETTER -> handleLetter(key)
            KeyType.DIGITAL -> handleDigitalKey(key)
            KeyType.SYMBOL -> {
                commitComposing()
                if (key.code.isNotEmpty()) {
                    val withBidi = addBidiIfNeeded(key.code[0])
                    listener.onKeyPress(withBidi)
                    lastChar = key.code[0]
                }
                autoCancelShift()
                resetKeyPressCounter()
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
            updatePinyinCandidates()
            listener.onComposingChanged(composing)
            return true
        }
        // 柯语联想（默认关）：进入 composing 缓冲，候选来自词库
        if (currentLanguage == Language.KYRGYZ && kyrgyzAssociationEnabled &&
            kyrgyzWordProvider != null && code.isNotEmpty()
        ) {
            composingBuilder.append(code)
            updateKyrgyzCandidates()
            listener.onComposingChanged(composing)
            autoCancelShift()
            return true
        }
        if (code.isNotEmpty()) {
            val withBidi = addBidiIfNeeded(code[0])
            val out = if (currentLanguage == Language.ENGLISH && englishShift) {
                withBidi.uppercase().also { if (!shiftLocked) englishShift = false }
            } else withBidi
            listener.onKeyPress(out)
            lastChar = code[0]
        }
        autoCancelShift()
        return true
    }

    /**
     * 处理数字键盘按键（GB/T 31921-2015 标准）。
     * 点击：输出数字
     * 500ms 内连按：循环切换输出该键 symbols 列表中的所有字符
     * 长按：输出第一个字母
     */
    private fun handleDigitalKey(key: KyrgyzKey): Boolean {
        val digit = key.label[0]
        val now = System.currentTimeMillis()
        val timeSinceLastPress = now - lastPressTime

        // 500ms 内连按同一个键：在“数字本身 + 各变体字符”之间循环替换
        if (digit == lastKey && timeSinceLastPress < 500 && key.variants.isNotEmpty()) {
            // 先删除上一次已提交的输出（含可能插入的双向控制符），实现原地替换
            if (lastEmittedLength > 0) {
                listener.onDeleteBeforeCursor(lastEmittedLength)
            }
            // 循环序列长度 = 数字(1) + 变体个数；0 表示数字本身
            keyPressCount = (keyPressCount + 1) % (key.variants.size + 1)
            val emitted: String = if (keyPressCount == 0) {
                lastChar = digit
                digit.toString()
            } else {
                val nextChar = key.variants[keyPressCount - 1]
                lastChar = nextChar[0]
                addBidiIfNeeded(nextChar[0])
            }
            listener.onKeyPress(emitted)
            lastEmittedLength = emitted.length
        } else {
            // 首次点击或超时：提交上一段并输出数字本身
            commitComposing()
            val emitted = digit.toString()
            listener.onKeyPress(emitted)
            keyPressCount = 0
            lastChar = digit
            lastEmittedLength = emitted.length
        }

        lastKey = digit
        lastPressTime = now

        return true
    }

    private fun handleFunction(fn: FunctionType) {
        when (fn) {
            FunctionType.SWITCH_LAYOUT -> {
                commitComposing()
                switchLayout()
            }
            FunctionType.SWITCH_DIGITAL -> {
                commitComposing()
                switchToDigital()
            }
            FunctionType.SWITCH_LANGUAGE -> {
                switchLanguage()
            }
            FunctionType.SHIFT -> {
                val now = System.currentTimeMillis()
                val isDoubleClick = (now - lastPressTime) < 300L

                if (currentLanguage == Language.KYRGYZ && currentLayout == LayoutType.DEFAULT) {
                    when {
                        isDoubleClick -> {
                            kyrgyzShift = true
                            shiftLocked = true
                        }
                        kyrgyzShift && shiftLocked -> {
                            kyrgyzShift = false
                            shiftLocked = false
                        }
                        else -> {
                            kyrgyzShift = !kyrgyzShift
                            shiftLocked = false
                        }
                    }
                } else if (currentLanguage == Language.ENGLISH) {
                    when {
                        isDoubleClick -> {
                            englishShift = true
                            shiftLocked = true
                        }
                        englishShift && shiftLocked -> {
                            englishShift = false
                            shiftLocked = false
                        }
                        else -> {
                            englishShift = !englishShift
                            shiftLocked = false
                        }
                    }
                }
                lastPressTime = now
            }
            FunctionType.BACKSPACE -> {
                if (composingBuilder.isNotEmpty()) {
                    composingBuilder.deleteCharAt(composingBuilder.length - 1)
                    when (currentLanguage) {
                        Language.PINYIN -> updatePinyinCandidates()
                        Language.KYRGYZ -> updateKyrgyzCandidates()
                        else -> {}
                    }
                    listener.onComposingChanged(composing)
                    return
                }
                listener.onKeyAction(FunctionType.BACKSPACE)
                autoCancelShift()
                lastChar = null
            }
            FunctionType.SPACE -> {
                if (currentLanguage == Language.PINYIN && candidates.isNotEmpty()) {
                    selectCandidate(0)
                    return
                }
                if (composingBuilder.isNotEmpty()) {
                    commitComposing()
                    return
                }
                listener.onKeyAction(FunctionType.SPACE)
                lastChar = ' '
                autoCancelShift()
                resetKeyPressCounter()
            }
            FunctionType.NEWLINE -> {
                commitComposing()
                listener.onKeyAction(FunctionType.NEWLINE)
                lastChar = '\n'
                autoCancelShift()
            }
            else -> {
                commitComposing()
                listener.onKeyAction(fn)
            }
        }
    }

    private fun autoCancelShift() {
        if (!shiftLocked && currentLayout != LayoutType.DIGITAL) {
            when (currentLanguage) {
                Language.KYRGYZ -> kyrgyzShift = false
                Language.ENGLISH -> englishShift = false
                else -> {}
            }
        }
    }

    fun selectCandidate(index: Int) {
        if (index !in candidates.indices) return
        val chosen = candidates[index]
        composingBuilder.clear()
        candidates = emptyList()
        listener.onCommitCandidate(chosen)
        if (currentLanguage == Language.KYRGYZ) kyrgyzWordProvider?.learn(chosen)
        resetKeyPressCounter()
        if (chosen.isNotEmpty()) {
            lastChar = chosen.last()
        }
    }

    fun handleVariant(variant: String): Boolean {
        if (variant.isEmpty()) return false
        commitComposing()
        val withBidi = addBidiIfNeeded(variant[0])
        listener.onKeyPress(withBidi)
        lastChar = variant[0]
        autoCancelShift()
        return true
    }

    /**
     * 长按数字键：直接输出 symbols 列表中的第一个字符。
     */
    fun handleDigitalLongPress(key: KyrgyzKey): Boolean {
        if (key.type != KeyType.DIGITAL || key.variants.isEmpty()) return false
        val firstChar = key.variants[0]
        commitComposing()
        val withBidi = addBidiIfNeeded(firstChar[0])
        listener.onKeyPress(withBidi)
        lastChar = firstChar[0]
        return true
    }

    private fun updatePinyinCandidates() {
        candidates = if (composingBuilder.isEmpty()) emptyList()
        else PinyinDictionary.query(composingBuilder.toString())
    }

    private fun updateKyrgyzCandidates() {
        val prefix = composingBuilder.toString()
        if (prefix.isEmpty()) {
            candidates = emptyList()
            return
        }
        val words = kyrgyzWordProvider?.query(prefix) ?: emptyList()
        candidates = when {
            words.isEmpty() -> listOf(prefix)
            words.contains(prefix) -> words
            else -> words + prefix
        }
    }


    private fun commitComposing() {
        if (composingBuilder.isNotEmpty()) {
            val text = if (candidates.isNotEmpty()) candidates[0] else composing
            composingBuilder.clear()
            candidates = emptyList()
            listener.onCommitCandidate(text)
            if (currentLanguage == Language.KYRGYZ) kyrgyzWordProvider?.learn(text)
            if (text.isNotEmpty()) {
                lastChar = text.last()
            }
        }
        resetKeyPressCounter()
    }

    private fun clearComposing() {
        if (composingBuilder.isNotEmpty()) {
            composingBuilder.clear()
            candidates = emptyList()
            listener.onComposingChanged("")
        }
    }

    private fun resetKeyPressCounter() {
        lastKey = null
        keyPressCount = 0
        lastPressTime = 0
        lastEmittedLength = 0
    }

    /**
     * GB/T 32412-2015 双向控制符自动插入
     */
    private fun addBidiIfNeeded(nextChar: Char): String {
        if (lastChar == null) return nextChar.toString()
        
        if (BidiControls.needsRlmAfter(lastChar.toString(), nextChar)) {
            return BidiControls.RLM + nextChar
        }
        
        if (BidiControls.needsLrmBefore(lastChar.toString(), nextChar)) {
            return BidiControls.LRM + nextChar
        }
        
        return nextChar.toString()
    }
}