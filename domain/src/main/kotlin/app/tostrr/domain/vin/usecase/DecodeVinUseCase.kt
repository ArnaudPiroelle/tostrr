package app.tostrr.domain.vin.usecase

import app.tostrr.domain.vin.VinCodeRepository
import app.tostrr.domain.vin.VinDecoder
import app.tostrr.domain.vin.VinDecoding
import org.koin.core.annotation.Factory

/** [vin] cut into its parts and decoded, or null when there is no VIN or it isn't a valid one. */
@Factory
class DecodeVinUseCase(private val repository: VinCodeRepository) {
    suspend operator fun invoke(vin: String?): VinDecoding? = vin?.let { VinDecoder.decode(it, repository.table()) }
}
