package app.tostrr.domain.vin

/** The table giving the meaning of Tesla VIN codes. */
interface VinCodeRepository {
    suspend fun table(): VinCodeTable
}
