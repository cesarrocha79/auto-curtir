package com.cesar.autocurtir

import android.content.Context

data class Config(
    val tapsMin: Int,
    val tapsMax: Int,
    val intervalMin: Int,
    val intervalMax: Int,
    val waitMin: Int,
    val waitMax: Int,
    val spread: Int,
    val maxVideos: Int
) {
    companion object {
        private const val PREFS = "cfg2"

        fun load(c: Context): Config {
            val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return Config(
                p.getInt("tapsMin", 30),
                p.getInt("tapsMax", 50),
                p.getInt("intMin", 120),
                p.getInt("intMax", 500),
                p.getInt("waitMin", 1500),
                p.getInt("waitMax", 3500),
                p.getInt("spread", 15),
                p.getInt("max", 0)
            )
        }

        fun save(c: Context, cfg: Config) {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("tapsMin", cfg.tapsMin)
                .putInt("tapsMax", cfg.tapsMax)
                .putInt("intMin", cfg.intervalMin)
                .putInt("intMax", cfg.intervalMax)
                .putInt("waitMin", cfg.waitMin)
                .putInt("waitMax", cfg.waitMax)
                .putInt("spread", cfg.spread)
                .putInt("max", cfg.maxVideos)
                .apply()
        }
    }
}
