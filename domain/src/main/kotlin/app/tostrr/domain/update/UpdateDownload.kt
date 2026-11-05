package app.tostrr.domain.update

/** Where the download of an update's APK stands. */
sealed interface UpdateDownload {
    /** [progress] goes from 0 to 1. */
    data class Downloading(val progress: Float) : UpdateDownload

    /** The complete APK is at [path], ready to hand to the system installer. */
    data class Ready(val path: String) : UpdateDownload
}
