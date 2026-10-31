package app.tostrr.domain.notification

/** The families of notifications the user can turn on or off, one per notification channel. */
enum class NotificationKind {
    /** Steps done or undone, configuration and delivery window changes. */
    PROGRESS,

    /** Appointment, VIN, plate, dates and final payment changes. */
    ALERTS,

    /** The days before the delivery appointment. */
    REMINDERS,

    /** Changes in Tesla's data that nothing else reports. */
    MINOR_UPDATES,

    /** The daily summary of the changes. */
    DIGEST,

    /** An expired session or a sync that stopped working. */
    SYNC_PROBLEMS,
}
