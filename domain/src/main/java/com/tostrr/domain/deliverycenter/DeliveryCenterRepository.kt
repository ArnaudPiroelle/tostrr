package com.tostrr.domain.deliverycenter

/** The known Tesla delivery centres. */
interface DeliveryCenterRepository {
    suspend fun centers(): DeliveryCenters
}
