package app.tostrr.domain.update

import app.tostrr.domain.update.ReleaseNotes.Block
import app.tostrr.domain.update.ReleaseNotes.Span
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {
    private fun plain(text: String) = listOf(Span(text))

    @Test fun `titles keep their level`() {
        assertEquals(
            listOf(Block.Heading(1, plain("Tostrr")), Block.Heading(3, plain("📦 Orders"))),
            ReleaseNotes.parse("# Tostrr\n\n### 📦 Orders"),
        )
    }

    @Test fun `the lines of a paragraph are joined, with CRLF too`() {
        assertEquals(
            listOf(Block.Paragraph(plain("one two")), Block.Paragraph(plain("three"))),
            ReleaseNotes.parse("one\r\ntwo\r\n\r\nthree"),
        )
    }

    @Test fun `bullets and numbers make list items`() {
        assertEquals(
            listOf(
                Block.ListItem("•", plain("dash")),
                Block.ListItem("•", plain("star")),
                Block.ListItem("2.", plain("number")),
            ),
            ReleaseNotes.parse("- dash\n* star\n2. number"),
        )
    }

    @Test fun `a GitHub alert becomes a plain quote`() {
        assertEquals(
            listOf(Block.Quote(plain("Unofficial app.")), Block.Paragraph(plain("After"))),
            ReleaseNotes.parse("> [!NOTE]\n> Unofficial app.\nAfter"),
        )
    }

    @Test fun `horizontal rules are left out`() {
        assertEquals(listOf(Block.Paragraph(plain("a")), Block.Paragraph(plain("b"))), ReleaseNotes.parse("a\n---\nb"))
    }

    @Test fun `bold, code and both are kept, and links keep their text`() {
        assertEquals(
            listOf(
                Block.Paragraph(
                    listOf(
                        Span("Get "),
                        Span("this", bold = true),
                        Span(" or "),
                        Span("that.apk", code = true),
                        Span(" or "),
                        Span("it.apk", bold = true, code = true),
                        Span(", see "),
                        Span("the page"),
                    ),
                ),
            ),
            ReleaseNotes.parse("Get **this** or `that.apk` or **`it.apk`**, see [the page](https://example.com)"),
        )
    }

    @Test fun `no notes, no blocks`() {
        assertEquals(emptyList<Block>(), ReleaseNotes.parse("\n \n"))
    }
}
