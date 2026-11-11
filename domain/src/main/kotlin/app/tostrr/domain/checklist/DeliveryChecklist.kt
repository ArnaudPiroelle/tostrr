package app.tostrr.domain.checklist

data class ChecklistQuestion(
    /** Stable key the answers are stored under, and that the app words the question by. */
    val id: String,
)

data class ChecklistSection(val id: String, val questions: List<ChecklistQuestion>)

enum class AnswerStatus { VERIFIED, SKIPPED }

data class ChecklistAnswer(
    val questionId: String,
    val status: AnswerStatus,
    val comment: String? = null,
    /** Absolute paths of the photos in the app's private storage, in the order they were added. */
    val photoPaths: List<String> = emptyList(),
    val answeredAtEpochMs: Long,
)

/** One screen of the walk-through: each section opens with an intro, then one screen per question. */
sealed interface ChecklistStep {
    val section: ChecklistSection

    data class SectionIntro(override val section: ChecklistSection) : ChecklistStep

    data class Question(
        override val section: ChecklistSection,
        val question: ChecklistQuestion,
        /** 1-based position in its section, for the "23 / 26" progress. */
        val number: Int,
    ) : ChecklistStep
}

/**
 * The delivery checklist of one order. [cursor] indexes [DeliveryChecklist.steps]; past the end means done. Once
 * [locked][lockedAtEpochMs], a completed checklist can't change any more, so its report says what was checked then.
 */
data class Checklist(
    val orderReference: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val cursor: Int,
    val answers: Map<String, ChecklistAnswer>,
    val lockedAtEpochMs: Long? = null,
) {
    val completed: Boolean get() = completedAtEpochMs != null

    val locked: Boolean get() = lockedAtEpochMs != null

    val verifiedCount: Int get() = answers.values.count { it.status == AnswerStatus.VERIFIED }

    /** Share of all the questions that were verified, 0–100. Skipped and unanswered ones count against it. */
    val verifiedPercent: Int get() = verifiedCount * 100 / DeliveryChecklist.questionCount
}

object DeliveryChecklist {
    /** The questions are worded by the app, from these ids (`<section>-<nn>`): keep them when rewording one. */
    val sections: List<ChecklistSection> = listOf(
        section("exterior", 26),
        section("interior", 14),
        section("software", 14),
        section("mechanical", 18),
        section("accessories", 9),
    )

    val steps: List<ChecklistStep> = sections.flatMap { section ->
        listOf(ChecklistStep.SectionIntro(section)) +
            section.questions.mapIndexed { i, q -> ChecklistStep.Question(section, q, i + 1) }
    }

    val questionCount: Int = sections.sumOf { it.questions.size }

    private val questionsById = sections.flatMap { it.questions }.associateBy { it.id }

    fun question(id: String): ChecklistQuestion? = questionsById[id]

    /** Index in [steps] of the screen asking [questionId], or -1 when there is none. */
    fun stepIndexOf(questionId: String): Int =
        steps.indexOfFirst { it is ChecklistStep.Question && it.question.id == questionId }

    private fun section(id: String, questions: Int) = ChecklistSection(
        id,
        (1..questions).map { ChecklistQuestion("$id-${it.toString().padStart(2, '0')}") },
    )
}
