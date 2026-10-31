package app.tostrr.domain.notification

/** Which notifications are posted: all of them, except the [disabled] kinds. */
data class NotificationSettings(val disabled: Set<NotificationKind> = emptySet()) {
    fun isEnabled(kind: NotificationKind) = kind !in disabled

    fun with(kind: NotificationKind, enabled: Boolean) = copy(disabled = if (enabled) disabled - kind else disabled + kind)
}
