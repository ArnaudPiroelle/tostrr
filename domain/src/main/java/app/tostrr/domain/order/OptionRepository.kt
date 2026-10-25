package app.tostrr.domain.order

import kotlinx.coroutines.flow.StateFlow

/** Labels of the option codes; empty until known, in which case screens show the bare codes. */
interface OptionRepository {
    val codes: StateFlow<Map<String, OptionInfo>>
}
