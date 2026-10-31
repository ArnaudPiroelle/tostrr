package app.tostrr.ui.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.order.Order

@Composable
internal fun VehicleContent(order: Order) {
    val info = order.vehicleInfo
    if (info.isEmpty) return Empty(stringResource(R.string.detail_vehicle_empty))
    KeyValue(stringResource(R.string.detail_vehicle_reserved_on), info.reservationDate)
    KeyValue(stringResource(R.string.detail_vehicle_booked_on), info.bookedDate)
    KeyValue(stringResource(R.string.detail_vehicle_plate), info.licensePlate)
    KeyValue(stringResource(R.string.detail_vehicle_odometer), info.odometer)
    KeyValue(stringResource(R.string.detail_vehicle_routing), info.routingLocation)
    KeyValue(stringResource(R.string.detail_vehicle_registration_status), info.registrationStatus)
}
