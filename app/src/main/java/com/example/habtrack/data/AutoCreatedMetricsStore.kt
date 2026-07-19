package com.example.habtrack.data

import android.content.Context

/**
 * Remembers which Health Connect metrics have ever had a habit in HabTrack, so auto-create never
 * resurrects a habit the user deleted. A metric is auto-created at most once; after that (or once
 * it's ever been tracked) it stays in this set and is skipped on every later sync.
 */
object AutoCreatedMetricsStore {
    private const val PREFS = "habtrack_hc_autocreate"
    private const val KEY = "created_metrics"

    fun get(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            ?.toSet() ?: emptySet()

    /** Unions [metrics] into the remembered set and persists. */
    fun add(context: Context, metrics: Set<String>) {
        if (metrics.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val merged = (prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()) + metrics
        prefs.edit().putStringSet(KEY, merged).apply()
    }
}
