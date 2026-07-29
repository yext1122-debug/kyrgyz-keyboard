package com.example.kyrgyzkeyboard.utils

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 按键触觉/听觉反馈。
 *
 * 由 IME Service 持有；[vibrateEnabled] / [soundEnabled] 跟随用户设置实时刷新。
 * 每次按键触发 [perform]，按开关决定是否震动、是否播放系统按键音。
 */
class KeyFeedback(context: Context) {

    private val appContext = context.applicationContext

    var vibrateEnabled: Boolean = true
    var soundEnabled: Boolean = false

    private val vibrator: Vibrator? by lazy { resolveVibrator() }
    private val audioManager: AudioManager? by lazy {
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    fun perform() {
        if (vibrateEnabled) vibrate()
        if (soundEnabled) playClick()
    }

    private fun vibrate() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(VIBRATE_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(VIBRATE_MS)
        }
    }

    private fun playClick() {
        audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, SOUND_VOLUME)
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        private const val VIBRATE_MS = 20L
        private const val SOUND_VOLUME = 0.5f
    }
}
