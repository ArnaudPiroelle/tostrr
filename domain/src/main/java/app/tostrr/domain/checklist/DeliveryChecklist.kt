package app.tostrr.domain.checklist

data class ChecklistQuestion(
    /** Stable key the answers are stored under: keep it when rewording a question. */
    val id: String,
    val text: String,
)

data class ChecklistSection(val id: String, val title: String, val questions: List<ChecklistQuestion>)

enum class AnswerStatus { VERIFIED, SKIPPED }

data class ChecklistAnswer(
    val questionId: String,
    val status: AnswerStatus,
    val comment: String? = null,
    /** Absolute path of the photo in the app's private storage. */
    val photoPath: String? = null,
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

/** The delivery checklist of one order. [cursor] indexes [DeliveryChecklist.steps]; past the end means done. */
data class Checklist(
    val orderReference: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val cursor: Int,
    val answers: Map<String, ChecklistAnswer>,
) {
    val completed: Boolean get() = completedAtEpochMs != null

    val verifiedCount: Int get() = answers.values.count { it.status == AnswerStatus.VERIFIED }

    /** Share of all the questions that were verified, 0–100. Skipped and unanswered ones count against it. */
    val verifiedPercent: Int get() = verifiedCount * 100 / DeliveryChecklist.questionCount
}

object DeliveryChecklist {
    val sections: List<ChecklistSection> = listOf(
        section(
            "exterior", "Extérieur",
            "Le numéro VIN sur le pare-brise correspond-il au certificat d'immatriculation ?",
            "Les deux plaques d'immatriculation sont-elles correctes et bien fixées ?",
            "La couleur et les options correspondent-elles aux documents ?",
            "La peinture est-elle exempte de rayures, bosses ou défauts du côté conducteur ?",
            "La peinture est-elle exempte de rayures, bosses ou défauts du côté passager ?",
            "La peinture est-elle exempte de rayures, bosses ou défauts à l'avant ?",
            "La peinture est-elle exempte de rayures, bosses ou défauts à l'arrière ?",
            "Les ouvertures des portes et des coffres sont-elles exemptes de rayures ?",
            "Les portes sont-elles bien alignées avec la carrosserie ?",
            "Les poignées de porte sont-elles parfaitement affleurantes à l'extérieur ?",
            "Le frunk avant est-il bien aligné ?",
            "Le hayon arrière est-il bien aligné ?",
            "Les écarts entre les panneaux sont-ils uniformes et corrects ?",
            "Les vitres sont-elles exemptes de rayures ou défauts ?",
            "Le toit en verre est-il correctement posé et exempt de défauts ?",
            "Le pare-brise est-il exempt de fissures ou imperfections ?",
            "Les quatre vitres s'ouvrent-elles et se ferment-elles correctement ?",
            "Les phares et LED sont-ils bien alignés avec la carrosserie ?",
            "Les phares et LED sont-ils exempts de condensation à l'intérieur ?",
            "Les jantes sont-elles exemptes de rayures ou dommages ?",
            "Les pneus sont-ils en bon état et les dates de fabrication (DOT) sont-elles récentes ?",
            "Les bas de caisse et le soubassement sont-ils exempts de dommages (vérifiez en vous baissant) ?",
            "Le dessous du châssis est-il exempt d'éraflures dues au transport ?",
            "Les passages de roues comportent-ils tous leurs clips, correctement fixés ?",
            "Les bouchons de valve sont-ils présents sur les 4 pneus ?",
            "Le plastique protecteur pour le remorquage à l'avant s'enlève-t-il facilement ?",
        ),
        section(
            "interior", "Intérieur",
            "Les sièges sont-ils exempts de traces, taches ou rayures ?",
            "Les réglages électriques des sièges avant fonctionnent-ils correctement ?",
            "Les réglages électriques de la banquette arrière (si applicable) fonctionnent-ils correctement ?",
            "Les garnitures en tissu sont-elles exemptes de traces, taches ou rayures ?",
            "Les plastiques intérieurs sont-ils exempts de traces, taches ou rayures ?",
            "Le volant est-il exempt de traces, taches ou rayures ?",
            "Les réglages électriques du volant fonctionnent-ils correctement ?",
            "Les commandes du volant et des commodos fonctionnent-elles correctement ?",
            "Les plafonniers et l'éclairage des pare-soleil fonctionnent-ils correctement ?",
            "Les trois tapis d'intérieur (deux avant, un arrière) sont-ils présents et en bon état ?",
            "Le ciel de toit est-il bien aligné, le textile bien en place partout et sans traces de doigts ?",
            "Le pare-brise intérieur et la planche de bord sont-ils sans traces de ventouses ?",
            "Les sièges avant sont-ils silencieux quand ils bougent dans toutes les directions ?",
            "Les sièges arrière sont-ils silencieux quand ils sont rabaissés ?",
        ),
        section(
            "software", "Logiciel et connectivité",
            "Les écrans sont-ils réactifs et exempts de rayures ?",
            "L'écran est-il sans pixels défectueux ?",
            "Le logiciel affiche-t-il correctement les informations (kilométrage, type de batterie, version logicielle) ?",
            "La voiture apparaît-elle dans l'application Tesla ?",
            "La connexion 4G/5G (icône LTE ou 5G) est-elle active ?",
            "Le GPS fonctionne-t-il correctement (testez une destination, par exemple votre domicile) ?",
            "Toutes les caméras (avant, arrière, latérales) fonctionnent-elles correctement (vérifiez via l'icône en bas de l'écran ou le mode Sentinelle) ?",
            "Le système audio (radio, par exemple) fonctionne-t-il correctement ?",
            "Le micro (activé via le bouton droit du volant) fonctionne-t-il correctement ?",
            "La fonction dashcam est-elle opérationnelle ?",
            "Le navigateur web fonctionne-t-il ?",
            "La connexion Bluetooth avec votre smartphone fonctionne-t-elle en entrée passive ?",
            "Toutes les fonctions de l'application mobile s'activent-elles correctement (ouverture de la trappe de charge, ventilation, climatisation) ?",
            "Avez-vous réinitialisé un compteur kilométrique renommé « DEPUIS ACHAT - NE PAS EFFACER » pour suivre la consommation depuis le début ?",
        ),
        section(
            "mechanical", "Fonctionnalités mécaniques et électriques",
            "Les portes s'ouvrent-elles et se ferment-elles correctement sans effort excessif ?",
            "Les coffres avant (frunk) et arrière s'ouvrent-ils et se ferment-ils correctement ?",
            "Les joints de porte sont-ils proprement installés et en bon état ?",
            "La boîte à gants s'ouvre-t-elle et se ferme-t-elle correctement ?",
            "Les deux cartes-clés fonctionnent-elles correctement ?",
            "Les ports de charge sont-ils en bon état et fonctionnent-ils correctement ?",
            "La charge actuelle est-elle suffisante pour atteindre votre destination ou un chargeur ?",
            "Le coffre et le sous-coffre sont-ils exempts d'eau ?",
            "Le coffre avant est-il exempt d'eau ?",
            "La trappe du port de charge est-elle alignée avec la carrosserie, et s'ouvre-t-elle et se ferme-t-elle facilement ?",
            "Le volant est-il aligné avec les roues ?",
            "Les rétroviseurs se règlent-ils et se rabattent-ils correctement, sans bruit ?",
            "Toutes les lumières fonctionnent-elles (clignotants, appels de phare, brouillard, freins, etc.) ?",
            "La climatisation et le chauffage fonctionnent-ils à plein régime sans bruit anormal ?",
            "La poignée d'ouverture d'urgence à l'avant fonctionne-t-elle ?",
            "Le bac de rangement central et la boîte à gants fonctionnent-ils ?",
            "L'allume-cigare permet-il de charger un appareil ?",
            "La charge rapide (DC) et la charge lente (AC) fonctionnent-elles ?",
        ),
        section(
            "accessories", "Accessoires et équipements",
            "La trousse de secours est-elle présente ?",
            "Le triangle de signalisation est-il présent ?",
            "Le câble de recharge Type 2 est-il présent ?",
            "La clé USB est-elle présente dans la boîte à gants ?",
            "[optionnel] Le chargeur secteur avec prise 230 V est-il présent ?",
            "[optionnel] L'attelage est-il présent avec sa sacoche ?",
            "[optionnel] La prise électrique de l'attelage est-elle en bon état ?",
            "L'accessoire sous le tapis du coffre avant (tige de remorquage) est-il présent, et y a-t-il du liquide lave-glace ?",
            "Le manuel de l'utilisateur est-il dans la boîte à gants ?",
        ),
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

    private fun section(id: String, title: String, vararg questions: String) = ChecklistSection(
        id,
        title,
        questions.mapIndexed { i, text -> ChecklistQuestion("$id-${(i + 1).toString().padStart(2, '0')}", text) },
    )
}
