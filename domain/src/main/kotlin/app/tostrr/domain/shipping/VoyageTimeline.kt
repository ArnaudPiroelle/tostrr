package app.tostrr.domain.shipping

/** Where a port stands on a ship's route. */
enum class PortStatus {
    /** The first port, left behind. */
    DEPARTED,

    /** A port called at and left. */
    PASSED,

    /** The port the ship lies in now. */
    BERTHED,

    /** The port it sails for. */
    NEXT,

    /** A port after the next one. */
    LATER,
}

/** A line of a ship's timeline: a port of its route, or the ship at sea between two of them. */
sealed interface TimelineEntry {
    data class Port(val name: String, val status: PortStatus) : TimelineEntry

    /** At sea, with the share of its route sailed (0 to 1) when the site gives it. */
    data class AtSea(val progress: Double?) : TimelineEntry
}

object VoyageTimeline {
    /**
     * [ship]'s route in order, with where it is: the ports before its next port passed, then the ship at sea before
     * that port, or lying in it. When its next port isn't on its route, only the first port counts as left.
     */
    fun of(ship: Ship): List<TimelineEntry> {
        val next = ship.route.indexOfFirst { it.equals(ship.nextPort, ignoreCase = true) }
        if (next < 0) {
            if (ship.route.size < 2) return ship.route.map { TimelineEntry.Port(it, PortStatus.LATER) }
            val atSea = if (ship.atBerth) emptyList() else listOf(TimelineEntry.AtSea(ship.progress))
            return listOf(TimelineEntry.Port(ship.route.first(), PortStatus.DEPARTED)) + atSea +
                ship.route.drop(1).map { TimelineEntry.Port(it, PortStatus.LATER) }
        }
        return ship.route.flatMapIndexed { index, name ->
            when {
                index < next -> listOf(TimelineEntry.Port(name, if (index == 0) PortStatus.DEPARTED else PortStatus.PASSED))
                index > next -> listOf(TimelineEntry.Port(name, PortStatus.LATER))
                ship.atBerth -> listOf(TimelineEntry.Port(name, PortStatus.BERTHED))
                else -> listOf(TimelineEntry.AtSea(ship.progress), TimelineEntry.Port(name, PortStatus.NEXT))
            }
        }
    }
}
