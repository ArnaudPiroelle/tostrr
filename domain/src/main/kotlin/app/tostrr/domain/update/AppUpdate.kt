package app.tostrr.domain.update

/** A published release newer than the installed app; [notes] is its Markdown text, as written on GitHub. */
data class AppUpdate(
    val version: String,
    val notes: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)
