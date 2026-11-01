package app.tostrr.domain.sync

/** The choices for the time between two background syncs. WorkManager won't run a periodic job more often than 15 minutes. */
enum class SyncInterval(val minutes: Long) {
    MINUTES_15(15),
    MINUTES_30(30),
    HOUR_1(60),
    HOURS_2(120),
    HOURS_6(360),
    ;

    companion object {
        val DEFAULT = MINUTES_15

        /** The choice for a stored value; one no longer offered falls back to [DEFAULT]. */
        fun fromMinutes(minutes: Long): SyncInterval = entries.firstOrNull { it.minutes == minutes } ?: DEFAULT
    }
}
