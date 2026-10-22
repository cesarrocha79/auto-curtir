package com.cesar.autocurtir

import android.content.Context

data class Config(
    val taps: Int,
    val tapIntervalMs: Int,
    val waitAfterSwipeMs: Int,
    val maxVideos: Int
) {
    companion object {
        private const val PREFS = "cfg"

        fun load(c: Context): Config {
            val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return Config(
                p.getInt("taps", 30),
                p.getInt("tapInt", 120),
                p.getInt("wait", 1500),
                p.getInt("max", 0)
            )
        }

        fun save(c: Context, cfg: Config) {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("taps", cfg.taps)
                .putInt("tapInt", cfg.tapIntervalMs)
                .putInt("wait", cfg.waitAfterSwipeMs)
                .putInt("max", cfg.maxVideos)
                .apply()
        }
    }
}
