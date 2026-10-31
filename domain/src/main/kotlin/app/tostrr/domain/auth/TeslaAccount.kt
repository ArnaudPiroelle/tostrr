package app.tostrr.domain.auth

/** Who the Tesla session belongs to, as the account reports it; [avatarPath] is the local copy of its picture, if any. */
data class TeslaAccount(
    val fullName: String,
    val avatarPath: String? = null,
)
