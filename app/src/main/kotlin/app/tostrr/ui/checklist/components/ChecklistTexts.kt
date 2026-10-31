package app.tostrr.ui.checklist.components

import androidx.annotation.StringRes
import app.tostrr.R
import app.tostrr.domain.checklist.ChecklistQuestion
import app.tostrr.domain.checklist.ChecklistSection

/** The words of the checklist, kept in `checklist_strings.xml` under the ids of `DeliveryChecklist`. */
@get:StringRes
internal val ChecklistSection.titleRes: Int
    get() = when (id) {
        "exterior" -> R.string.checklist_section_exterior
        "interior" -> R.string.checklist_section_interior
        "software" -> R.string.checklist_section_software
        "mechanical" -> R.string.checklist_section_mechanical
        "accessories" -> R.string.checklist_section_accessories
        else -> error("Unknown checklist section $id")
    }

@get:StringRes
internal val ChecklistQuestion.textRes: Int
    get() = when (id) {
        "exterior-01" -> R.string.checklist_q_exterior_01
        "exterior-02" -> R.string.checklist_q_exterior_02
        "exterior-03" -> R.string.checklist_q_exterior_03
        "exterior-04" -> R.string.checklist_q_exterior_04
        "exterior-05" -> R.string.checklist_q_exterior_05
        "exterior-06" -> R.string.checklist_q_exterior_06
        "exterior-07" -> R.string.checklist_q_exterior_07
        "exterior-08" -> R.string.checklist_q_exterior_08
        "exterior-09" -> R.string.checklist_q_exterior_09
        "exterior-10" -> R.string.checklist_q_exterior_10
        "exterior-11" -> R.string.checklist_q_exterior_11
        "exterior-12" -> R.string.checklist_q_exterior_12
        "exterior-13" -> R.string.checklist_q_exterior_13
        "exterior-14" -> R.string.checklist_q_exterior_14
        "exterior-15" -> R.string.checklist_q_exterior_15
        "exterior-16" -> R.string.checklist_q_exterior_16
        "exterior-17" -> R.string.checklist_q_exterior_17
        "exterior-18" -> R.string.checklist_q_exterior_18
        "exterior-19" -> R.string.checklist_q_exterior_19
        "exterior-20" -> R.string.checklist_q_exterior_20
        "exterior-21" -> R.string.checklist_q_exterior_21
        "exterior-22" -> R.string.checklist_q_exterior_22
        "exterior-23" -> R.string.checklist_q_exterior_23
        "exterior-24" -> R.string.checklist_q_exterior_24
        "exterior-25" -> R.string.checklist_q_exterior_25
        "exterior-26" -> R.string.checklist_q_exterior_26
        "interior-01" -> R.string.checklist_q_interior_01
        "interior-02" -> R.string.checklist_q_interior_02
        "interior-03" -> R.string.checklist_q_interior_03
        "interior-04" -> R.string.checklist_q_interior_04
        "interior-05" -> R.string.checklist_q_interior_05
        "interior-06" -> R.string.checklist_q_interior_06
        "interior-07" -> R.string.checklist_q_interior_07
        "interior-08" -> R.string.checklist_q_interior_08
        "interior-09" -> R.string.checklist_q_interior_09
        "interior-10" -> R.string.checklist_q_interior_10
        "interior-11" -> R.string.checklist_q_interior_11
        "interior-12" -> R.string.checklist_q_interior_12
        "interior-13" -> R.string.checklist_q_interior_13
        "interior-14" -> R.string.checklist_q_interior_14
        "software-01" -> R.string.checklist_q_software_01
        "software-02" -> R.string.checklist_q_software_02
        "software-03" -> R.string.checklist_q_software_03
        "software-04" -> R.string.checklist_q_software_04
        "software-05" -> R.string.checklist_q_software_05
        "software-06" -> R.string.checklist_q_software_06
        "software-07" -> R.string.checklist_q_software_07
        "software-08" -> R.string.checklist_q_software_08
        "software-09" -> R.string.checklist_q_software_09
        "software-10" -> R.string.checklist_q_software_10
        "software-11" -> R.string.checklist_q_software_11
        "software-12" -> R.string.checklist_q_software_12
        "software-13" -> R.string.checklist_q_software_13
        "software-14" -> R.string.checklist_q_software_14
        "mechanical-01" -> R.string.checklist_q_mechanical_01
        "mechanical-02" -> R.string.checklist_q_mechanical_02
        "mechanical-03" -> R.string.checklist_q_mechanical_03
        "mechanical-04" -> R.string.checklist_q_mechanical_04
        "mechanical-05" -> R.string.checklist_q_mechanical_05
        "mechanical-06" -> R.string.checklist_q_mechanical_06
        "mechanical-07" -> R.string.checklist_q_mechanical_07
        "mechanical-08" -> R.string.checklist_q_mechanical_08
        "mechanical-09" -> R.string.checklist_q_mechanical_09
        "mechanical-10" -> R.string.checklist_q_mechanical_10
        "mechanical-11" -> R.string.checklist_q_mechanical_11
        "mechanical-12" -> R.string.checklist_q_mechanical_12
        "mechanical-13" -> R.string.checklist_q_mechanical_13
        "mechanical-14" -> R.string.checklist_q_mechanical_14
        "mechanical-15" -> R.string.checklist_q_mechanical_15
        "mechanical-16" -> R.string.checklist_q_mechanical_16
        "mechanical-17" -> R.string.checklist_q_mechanical_17
        "mechanical-18" -> R.string.checklist_q_mechanical_18
        "accessories-01" -> R.string.checklist_q_accessories_01
        "accessories-02" -> R.string.checklist_q_accessories_02
        "accessories-03" -> R.string.checklist_q_accessories_03
        "accessories-04" -> R.string.checklist_q_accessories_04
        "accessories-05" -> R.string.checklist_q_accessories_05
        "accessories-06" -> R.string.checklist_q_accessories_06
        "accessories-07" -> R.string.checklist_q_accessories_07
        "accessories-08" -> R.string.checklist_q_accessories_08
        "accessories-09" -> R.string.checklist_q_accessories_09
        else -> error("Unknown checklist question $id")
    }
