package com.tostrr.domain.deliverycenter.usecase

import com.tostrr.domain.deliverycenter.DeliveryCenter
import com.tostrr.domain.deliverycenter.DeliveryCenterRepository
import org.koin.core.annotation.Factory

/** The known delivery centre matching the name and country Tesla gives for an order, if any. */
@Factory
class FindDeliveryCenterUseCase(private val repository: DeliveryCenterRepository) {
    suspend operator fun invoke(name: String?, country: String?): DeliveryCenter? = repository.centers().find(name, country)
}
