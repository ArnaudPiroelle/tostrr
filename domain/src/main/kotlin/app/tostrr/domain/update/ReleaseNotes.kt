package app.tostrr.domain.update

/**
 * Reads the Markdown a GitHub release is written in, down to what the update screen shows: titles, paragraphs, lists,
 * quotes, bold and code. Links keep only their text, as they can't be tapped; anything else stays as written.
 */
object ReleaseNotes {
    data class Span(val text: String, val bold: Boolean = false, val code: Boolean = false)

    sealed interface Block {
        val spans: List<Span>

        data class Heading(val level: Int, override val spans: List<Span>) : Block
        data class Paragraph(override val spans: List<Span>) : Block
        data class Quote(override val spans: List<Span>) : Block

        /** [marker] is `•` or the item's number, `1.`. */
        data class ListItem(val marker: String, override val spans: List<Span>) : Block
    }

    private val HEADING = Regex("""(#{1,6})\s+(.*)""")
    private val LIST_ITEM = Regex("""([-*+]|\d+\.)\s+(.*)""")
    private val RULE = Regex("""([-*_])\1{2,}""")
    private val ALERT = Regex("""\[!\w+]""")
    private val INLINE = Regex("""\*\*(.+?)\*\*|`(.+?)`|\[(.+?)]\(.+?\)""")

    fun parse(markdown: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val paragraph = mutableListOf<String>()
        val quote = mutableListOf<String>()

        fun flush() {
            if (paragraph.isNotEmpty()) blocks += Block.Paragraph(spans(paragraph.joinToString(" ")))
            if (quote.isNotEmpty()) blocks += Block.Quote(spans(quote.joinToString(" ")))
            paragraph.clear()
            quote.clear()
        }

        markdown.lineSequence().map { it.trim() }.forEach { line ->
            val heading = HEADING.matchEntire(line)
            val item = LIST_ITEM.matchEntire(line)
            when {
                line.startsWith(">") -> {
                    if (paragraph.isNotEmpty()) flush()
                    // GitHub's `> [!NOTE]` only picks a colour for the quote it opens.
                    line.removePrefix(">").trim().takeUnless { it.isEmpty() || ALERT.matches(it) }?.let { quote += it }
                }
                line.isEmpty() || RULE.matches(line) -> flush()
                heading != null -> {
                    flush()
                    blocks += Block.Heading(heading.groupValues[1].length, spans(heading.groupValues[2]))
                }
                item != null -> {
                    flush()
                    val marker = item.groupValues[1].takeIf { it.endsWith(".") } ?: "•"
                    blocks += Block.ListItem(marker, spans(item.groupValues[2]))
                }
                else -> {
                    if (quote.isNotEmpty()) flush()
                    paragraph += line
                }
            }
        }
        flush()
        return blocks
    }

    private fun spans(text: String, bold: Boolean = false): List<Span> = buildList {
        var position = 0
        for (match in INLINE.findAll(text)) {
            if (match.range.first > position) add(Span(text.substring(position, match.range.first), bold))
            val (strong, code, link) = match.destructured
            when {
                strong.isNotEmpty() -> addAll(spans(strong, bold = true))
                code.isNotEmpty() -> add(Span(code, bold, code = true))
                else -> addAll(spans(link, bold))
            }
            position = match.range.last + 1
        }
        if (position < text.length) add(Span(text.substring(position), bold))
    }
}
