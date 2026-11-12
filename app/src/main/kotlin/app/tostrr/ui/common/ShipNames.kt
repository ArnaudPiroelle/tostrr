package app.tostrr.ui.common

/** `GLOVIS LIGHTHOUSE` → `Glovis Lighthouse`; short words are kept as initials (`CMA CGM MONACO` → `CMA CGM Monaco`). */
internal fun shipDisplayName(name: String): String =
    name.split(' ').filter { it.isNotEmpty() }.joinToString(" ") { word ->
        if (word.length <= 3 && word.all { it.isLetter() }) word.uppercase() else capitalized(word)
    }

/** `PORT KEMBLA` → `Port Kembla`. */
internal fun portDisplayName(name: String): String =
    name.split(' ').filter { it.isNotEmpty() }.joinToString(" ", transform = ::capitalized)

private fun capitalized(word: String) = word.lowercase().replaceFirstChar { it.uppercase() }
