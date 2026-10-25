package app.tostrr.domain.order

/** Human description of a Tesla option code (e.g. `W38A` -> `18" Photon Wheels`). */
data class OptionInfo(val label: String, val category: String?)
