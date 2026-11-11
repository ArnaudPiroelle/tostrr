package app.tostrr.domain.shipping

import app.tostrr.domain.order.Order

/** A port of call; [passed] once the ship has left it for a later one. */
data class VoyagePort(val name: String, val position: GeoPosition, val passed: Boolean)

/** One ship's voyage: its known [ports] and the sea [lane] through them. */
data class Voyage(val ship: Ship, val ports: List<VoyagePort>, val lane: List<GeoPosition>)

/**
 * The ships that may carry an order's car, from the VIN block they are said to carry: an estimate, the site's own.
 * Several [candidates] while the site gives the block to more than one sailing ship, in the snapshot's order.
 */
data class ShipVoyage(
    val candidates: List<Voyage>,
    val vinBlock: String,
    val snapshotAtEpochMs: Long,
) {
    /** More than one ship may carry the car. */
    val uncertain: Boolean get() = candidates.size > 1
}

object ShipVoyages {
    /**
     * Whether [vin] is one the tracking site can place: it follows the ships taking Shanghai-built cars (VINs from
     * `LRW`) to Europe.
     */
    fun tracks(vin: String?): Boolean = vin != null && vin.length == 17 && vin.startsWith(SHANGHAI_WMI)

    /** The block of [vin] the site lists ships by: the first three digits of its six-digit serial. */
    fun vinBlock(vin: String): String = vin.takeLast(6).take(3)

    /**
     * The ships that may carry [order]'s car, while there is one to follow: a VIN the site tracks, not delivered yet,
     * and at least one ship still sailing with its block.
     */
    fun of(order: Order, snapshot: ShipSnapshot?): ShipVoyage? {
        val vin = order.vin?.takeIf(::tracks) ?: return null
        if (snapshot == null || order.delivered) return null
        val block = vinBlock(vin)
        val ships = snapshot.ships.filter { !it.done && block in it.vinBlocks }
        if (ships.isEmpty()) return null
        return ShipVoyage(
            candidates = ships.map { Voyage(it, ports(it), SeaLanes.lane(it.route)) },
            vinBlock = block,
            snapshotAtEpochMs = snapshot.takenAtEpochMs,
        )
    }

    /** The ports of [ship] we can place, those before its next port passed. */
    private fun ports(ship: Ship): List<VoyagePort> {
        val next = ship.route.indexOfFirst { it.equals(ship.nextPort, ignoreCase = true) }
        return ship.route.mapIndexedNotNull { index, name ->
            SeaLanes.ports[name.uppercase()]?.let { VoyagePort(name, it, passed = next >= 0 && index < next) }
        }
    }

    private const val SHANGHAI_WMI = "LRW"
}
