package app.tostrr.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import app.tostrr.R
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.ChecklistReportWriter
import app.tostrr.domain.checklist.DeliveryChecklist
import app.tostrr.domain.order.Order
import app.tostrr.ui.checklist.components.textRes
import app.tostrr.ui.checklist.components.titleRes
import app.tostrr.ui.common.formatDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File

/**
 * The checklist report as an A4 PDF in the app's language: the car, the counts, then every question with its answer,
 * comment and photos. Written to `cacheDir/reports`, which the FileProvider shares.
 */
@Single
class ChecklistReportPdf(private val context: Context) : ChecklistReportWriter {

    override suspend fun write(checklist: Checklist, order: Order?): File = withContext(Dispatchers.IO) {
        val file = File(File(context.cacheDir, "reports").apply { mkdirs() }, "checklist-${checklist.orderReference.filter(Char::isLetterOrDigit)}.pdf")
        val document = PdfDocument()
        try {
            Writer(document).apply {
                header(checklist, order)
                body(checklist)
                finish()
            }
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        file
    }

    /** Lays the report out top to bottom, starting a new page whenever the next block doesn't fit. */
    private inner class Writer(private val document: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = 0f
        private val canvas: Canvas get() = page!!.canvas

        private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

        fun header(checklist: Checklist, order: Order?) {
            newPage()
            text(string(R.string.checklist_title), titlePaint)
            y += 4
            val car = listOfNotNull(order?.modelName, string(R.string.checklist_report_order, checklist.orderReference)).joinToString(" · ")
            text(car, bodyPaint)
            order?.vin?.let { text(string(R.string.order_vin, it), bodyPaint) }
            checklist.completedAtEpochMs?.let { text(string(R.string.checklist_report_completed, formatDateTime(it)), mutedPaint) }
            checklist.lockedAtEpochMs?.let { text(string(R.string.checklist_locked_on, formatDateTime(it)), mutedPaint) }
            y += 8
            val skipped = checklist.answers.values.count { it.status == AnswerStatus.SKIPPED }
            val total = DeliveryChecklist.questionCount
            val handled = checklist.verifiedCount + checklist.problemCount + skipped
            text(string(R.string.checklist_report_counts, checklist.verifiedCount, checklist.problemCount, skipped, total - handled, total), strongPaint)
            y += 12
            problems(checklist)
        }

        /** The problems first, each with its section and comment, so the delivery centre sees them at once. */
        private fun problems(checklist: Checklist) {
            val problems = DeliveryChecklist.sections.flatMap { section ->
                section.questions.mapNotNull { q -> checklist.answers[q.id]?.takeIf { it.status == AnswerStatus.PROBLEM }?.let { section to (q to it) } }
            }
            if (problems.isEmpty()) return
            ensure(sectionPaint.textSize + bodyPaint.textSize * 3 + 24)
            text(string(R.string.checklist_report_problems), TextPaint(sectionPaint).apply { color = NEGATIVE })
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
            y += 8
            problems.forEach { (section, entry) ->
                val (question, answer) = entry
                val line = layout("${string(section.titleRes)} — ${string(question.textRes)}", bodyPaint, CONTENT_WIDTH)
                val comment = answer.comment?.let { layout(it, commentPaint, CONTENT_WIDTH) }
                ensure(line.height + (comment?.height ?: 0) + 6f)
                draw(line, MARGIN)
                y += line.height
                comment?.let {
                    y += 2
                    draw(it, MARGIN)
                    y += it.height
                }
                y += 6
            }
            y += 6
        }

        fun body(checklist: Checklist) {
            DeliveryChecklist.sections.forEach { section ->
                val verified = section.questions.count { checklist.answers[it.id]?.status == AnswerStatus.VERIFIED }
                // A section title never ends a page on its own.
                ensure(sectionPaint.textSize + bodyPaint.textSize * 3 + 24)
                y += 8
                text("${string(section.titleRes)} · ${string(R.string.checklist_progress, verified, section.questions.size)}", sectionPaint)
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
                y += 8
                section.questions.forEach { question -> question(string(question.textRes), checklist.answers[question.id]) }
            }
        }

        private fun question(text: String, answer: app.tostrr.domain.checklist.ChecklistAnswer?) {
            val (label, color) = when (answer?.status) {
                AnswerStatus.VERIFIED -> string(R.string.checklist_answer_verified) to POSITIVE
                AnswerStatus.PROBLEM -> string(R.string.checklist_answer_problem) to NEGATIVE
                AnswerStatus.SKIPPED -> string(R.string.checklist_answer_skipped) to MUTED
                null -> string(R.string.checklist_answer_none) to MUTED
            }
            val statusPaint = TextPaint(strongPaint).apply { this.color = color }
            val textLayout = layout(text, bodyPaint, CONTENT_WIDTH - STATUS_WIDTH)
            val commentLayout = answer?.comment?.let { layout(it, commentPaint, CONTENT_WIDTH - STATUS_WIDTH) }
            ensure(textLayout.height + (commentLayout?.height ?: 0) + 6f)
            draw(layout(label, statusPaint, STATUS_WIDTH - 8), MARGIN)
            draw(textLayout, MARGIN + STATUS_WIDTH)
            y += textLayout.height
            commentLayout?.let {
                y += 2
                draw(it, MARGIN + STATUS_WIDTH)
                y += it.height
            }
            y += 6
            answer?.photoPaths?.takeIf { it.isNotEmpty() }?.let(::photos)
        }

        /** The photos in rows of [PHOTOS_PER_ROW], each whole in a 4:3 cell: a report must not crop what was seen. */
        private fun photos(paths: List<String>) {
            val width = (CONTENT_WIDTH - STATUS_WIDTH - PHOTO_GAP * (PHOTOS_PER_ROW - 1)) / PHOTOS_PER_ROW
            val height = width * 3 / 4
            paths.chunked(PHOTOS_PER_ROW).forEach { row ->
                ensure(height + PHOTO_GAP)
                row.forEachIndexed { i, path ->
                    val left = MARGIN + STATUS_WIDTH + i * (width + PHOTO_GAP)
                    val target = RectF(left, y, left + width, y + height)
                    val bitmap = decode(path, (width * 2).toInt())
                    if (bitmap == null) {
                        canvas.drawRect(target, rulePaint)
                    } else {
                        canvas.drawBitmap(bitmap, null, fitIn(bitmap, target), imagePaint)
                        bitmap.recycle()
                    }
                }
                y += height + PHOTO_GAP
            }
            y += 4
        }

        fun finish() {
            page?.let(::closePage)
        }

        private fun text(value: String, paint: TextPaint) {
            val layout = layout(value, paint, CONTENT_WIDTH)
            ensure(layout.height.toFloat())
            draw(layout, MARGIN)
            y += layout.height
        }

        private fun draw(layout: StaticLayout, x: Float) {
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }

        private fun ensure(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN - FOOTER) newPage()
        }

        private fun newPage() {
            page?.let(::closePage)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create())
            y = MARGIN
        }

        private fun closePage(page: PdfDocument.Page) {
            val footer = string(R.string.checklist_report_page, pageNumber)
            page.canvas.drawText(footer, PAGE_WIDTH - MARGIN - mutedPaint.measureText(footer), PAGE_HEIGHT - MARGIN / 2, mutedPaint)
            document.finishPage(page)
            this.page = null
        }
    }

    private fun layout(text: String, paint: TextPaint, width: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt()).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

    /** The largest rectangle with [bitmap]'s proportions inside [target], centred. */
    private fun fitIn(bitmap: Bitmap, target: RectF): RectF {
        val scale = minOf(target.width() / bitmap.width, target.height() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val left = target.left + (target.width() - w) / 2
        val top = target.top + (target.height() - h) / 2
        return RectF(left, top, left + w, top + h)
    }

    /** The photo at about [targetWidth] pixels wide, turned upright as the camera recorded it; null if unreadable. */
    private fun decode(path: String, targetWidth: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidth && bounds.outHeight / (sample * 2) >= targetWidth * 3 / 4) sample *= 2
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val degrees = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) bitmap
        else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
            .also { if (it != bitmap) bitmap.recycle() }
    }.getOrNull()

    private fun paint(size: Float, color: Int = INK, style: Int = Typeface.NORMAL) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, style)
    }

    private val titlePaint = paint(20f, style = Typeface.BOLD)
    private val sectionPaint = paint(14f, style = Typeface.BOLD)
    private val strongPaint = paint(10f, style = Typeface.BOLD)
    private val bodyPaint = paint(10f)
    private val mutedPaint = paint(9f, MUTED)
    private val commentPaint = paint(9f, MUTED, Typeface.ITALIC)
    private val rulePaint = Paint().apply { color = RULE; strokeWidth = 1f }
    private val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private companion object {
        // A4 in PostScript points.
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val MARGIN = 40f
        const val FOOTER = 16f
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        const val STATUS_WIDTH = 72f
        const val PHOTOS_PER_ROW = 3
        const val PHOTO_GAP = 6f
        val INK = Color.rgb(0x17, 0x1A, 0x20)
        val MUTED = Color.rgb(0x6B, 0x72, 0x80)
        val POSITIVE = Color.rgb(0x1F, 0x8A, 0x4C)
        val NEGATIVE = Color.rgb(0xC8, 0x10, 0x2E)
        val RULE = Color.rgb(0xD9, 0xDC, 0xE1)
    }
}
