package app.tostrr.domain.update

/** Compares the `1.2.0` / `1.2.0-beta.1` version names the releases use. */
object AppVersions {
    private val FORMAT = Regex("""(\d+(?:\.\d+)*)(-.+)?""")

    /** True if [candidate] comes after [installed]; a name that can't be read is never newer. */
    fun isNewer(candidate: String, installed: String): Boolean {
        val new = FORMAT.matchEntire(candidate) ?: return false
        val old = FORMAT.matchEntire(installed) ?: return false
        val newNumbers = new.numbers()
        val oldNumbers = old.numbers()
        for (i in 0 until maxOf(newNumbers.size, oldNumbers.size)) {
            val difference = newNumbers.getOrElse(i) { 0 }.compareTo(oldNumbers.getOrElse(i) { 0 })
            if (difference != 0) return difference > 0
        }
        // Same numbers: only the final version comes after its own pre-release.
        return new.isFinal() && !old.isFinal()
    }

    private fun MatchResult.numbers() = groupValues[1].split('.').map { it.toLongOrNull() ?: 0 }

    private fun MatchResult.isFinal() = groupValues[2].isEmpty()
}
