package com.tostrr.domain.order.usecase

import com.tostrr.domain.order.OptionInfo
import com.tostrr.domain.order.OptionRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** Labels of the option codes, empty until known. */
@Factory
class ObserveOptionCodesUseCase(private val repository: OptionRepository) {
    operator fun invoke(): StateFlow<Map<String, OptionInfo>> = repository.codes
}
