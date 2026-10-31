package app.tostrr.domain.auth

/** Who the Tesla session belongs to, as the account reports it; [avatarUrl] is null when it has no picture. */
data class TeslaAccount(
    val fullName: String,
    val avatarUrl: String? = null,
)
