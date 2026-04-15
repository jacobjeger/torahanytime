package com.torahanytime.audio.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists per-speaker playback speeds using SharedPreferences.
 * Each speaker ID maps to a float speed (e.g., 1.0, 1.25, 1.5, 1.75, 2.0).
 * Falls back to 1.0 for unknown speakers.
 */
object SpeakerSpeedStore {

    private const val PREFS_NAME = "speaker_speeds"
    private const val DEFAULT_SPEED = 1f

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get the saved playback speed for a speaker.
     * Returns DEFAULT_SPEED if no speed has been set.
     */
    fun getSpeed(speakerId: Int): Float {
        return prefs?.getFloat("speaker_$speakerId", DEFAULT_SPEED) ?: DEFAULT_SPEED
    }

    /**
     * Save the playback speed for a speaker.
     */
    fun setSpeed(speakerId: Int, speed: Float) {
        prefs?.edit()?.putFloat("speaker_$speakerId", speed)?.apply()
    }

    /**
     * Get speed for a speaker, or a default if speaker ID is null.
     */
    fun getSpeedOrDefault(speakerId: Int?, defaultSpeed: Float = DEFAULT_SPEED): Float {
        if (speakerId == null) return defaultSpeed
        return getSpeed(speakerId)
    }
}
