package app.tostrr.domain.update

/** What a check asked for by the user found. */
enum class UpdateCheck {
    /** The latest release is the installed one, or older. */
    UP_TO_DATE,

    /** A newer release is out, now in [UpdateRepository.update]. */
    AVAILABLE,

    /** GitHub couldn't be reached or gave nothing readable; what was known is kept. */
    FAILED,
}
