package com.omnkey.keyboard.core.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool

object SoundHelper {

    private var soundPool: SoundPool? = null
    private var soundMap = mutableMapOf<String, Int>()
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds from assets (would need actual sound files)
        // soundMap["click"] = soundPool?.load(context, R.raw.click, 1) ?: 0
        // soundMap["mechanical"] = soundPool?.load(context, R.raw.mechanical, 1) ?: 0
        // soundMap["space"] = soundPool?.load(context, R.raw.space, 1) ?: 0

        isInitialized = true
    }

    fun playKeySound(context: Context, volume: Int, mechanical: Boolean = false) {
        if (!isInitialized) initialize(context)

        val volumeFloat = volume / 100f
        val soundId = if (mechanical) {
            soundMap["mechanical"] ?: soundMap["click"] ?: return
        } else {
            soundMap["click"] ?: return
        }

        soundPool?.play(soundId, volumeFloat, volumeFloat, 1, 0, 1f)
    }

    fun playSpaceSound(context: Context, volume: Int) {
        if (!isInitialized) initialize(context)

        val volumeFloat = volume / 100f
        val soundId = soundMap["space"] ?: soundMap["click"] ?: return

        soundPool?.play(soundId, volumeFloat, volumeFloat, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isInitialized = false
    }
}
