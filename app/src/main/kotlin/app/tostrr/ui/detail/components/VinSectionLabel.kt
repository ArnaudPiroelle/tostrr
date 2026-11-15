package app.tostrr.ui.detail.components

import androidx.annotation.StringRes
import app.tostrr.R
import app.tostrr.domain.vin.VinSection

/** What a part of the VIN is, as the decoder's rows title it. */
@get:StringRes
internal val VinSection.labelRes: Int
    get() = when (this) {
        VinSection.MANUFACTURER -> R.string.detail_vin_section_manufacturer
        VinSection.MODEL -> R.string.detail_vin_section_model
        VinSection.BODY -> R.string.detail_vin_section_body
        VinSection.RESTRAINTS -> R.string.detail_vin_section_restraints
        VinSection.BATTERY -> R.string.detail_vin_section_battery
        VinSection.MOTOR -> R.string.detail_vin_section_motor
        VinSection.CHECK_DIGIT -> R.string.detail_vin_section_check_digit
        VinSection.MODEL_YEAR -> R.string.detail_vin_section_model_year
        VinSection.FACTORY -> R.string.detail_vin_section_factory
        VinSection.SERIAL -> R.string.detail_vin_section_serial
    }
