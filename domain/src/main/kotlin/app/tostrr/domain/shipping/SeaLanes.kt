package app.tostrr.domain.shipping

import java.util.PriorityQueue
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Where the tracked ships call, and a rough net of sea lanes between them, to draw a voyage on a map. The lanes go round
 * the Cape of Good Hope, not through Suez: car carriers have been avoiding the Red Sea. Approximate by design: they only
 * keep the drawn line off the land.
 */
object SeaLanes {
    /** The ports by the names the tracking site uses. */
    val ports: Map<String, GeoPosition> = mapOf(
        "NANGANG" to GeoPosition(30.88, 121.95),
        "SHANGHAI" to GeoPosition(31.35, 121.62),
        "HAITONG" to GeoPosition(31.36, 121.60),
        "YIZHENG" to GeoPosition(32.25, 119.18),
        "GUNSAN" to GeoPosition(35.98, 126.60),
        "PYEONGTAEK" to GeoPosition(36.97, 126.83),
        "MASAN" to GeoPosition(35.19, 128.57),
        "SINGAPORE" to GeoPosition(1.26, 103.82),
        "MAURITIUS" to GeoPosition(-20.16, 57.50),
        "PORT KEMBLA" to GeoPosition(-34.47, 150.90),
        "CORINTO" to GeoPosition(12.48, -87.18),
        "BALBOA" to GeoPosition(8.95, -79.57),
        "LAS PALMAS" to GeoPosition(28.14, -15.41),
        "CASABLANCA" to GeoPosition(33.61, -7.61),
        "BARCELONA" to GeoPosition(41.35, 2.16),
        "MISURATA" to GeoPosition(32.37, 15.21),
        "KOPER" to GeoPosition(45.55, 13.73),
        "DERINCE" to GeoPosition(40.75, 29.83),
        "CONSTANTA" to GeoPosition(44.17, 28.66),
        "ZEEBRUGGE" to GeoPosition(51.33, 3.20),
        "ROTTERDAM" to GeoPosition(51.95, 4.10),
        "AMSTERDAM" to GeoPosition(52.41, 4.79),
        "SOUTHAMPTON" to GeoPosition(50.90, -1.40),
        "BRISTOL" to GeoPosition(51.50, -2.71),
    )

    private val waypoints = mapOf(
        "YANGTZE" to GeoPosition(31.0, 122.6),
        "EAST_CHINA_SEA" to GeoPosition(29.0, 124.0),
        "YELLOW_SEA" to GeoPosition(34.5, 124.5),
        "KOREA_STRAIT" to GeoPosition(33.6, 127.5),
        "JAPAN_SOUTH" to GeoPosition(30.5, 132.0),
        "TAIWAN_STRAIT" to GeoPosition(24.5, 119.8),
        "SOUTH_CHINA_SEA_N" to GeoPosition(20.0, 116.0),
        "SOUTH_CHINA_SEA_S" to GeoPosition(8.5, 109.5),
        "SINGAPORE_STRAIT" to GeoPosition(1.6, 104.8),
        "MALACCA" to GeoPosition(2.5, 101.0),
        "SUMATRA_NORTH" to GeoPosition(6.0, 95.0),
        "INDIAN_OCEAN" to GeoPosition(-5.0, 80.0),
        "MASCARENES" to GeoPosition(-20.0, 60.0),
        "MADAGASCAR_SOUTH" to GeoPosition(-27.0, 46.0),
        "DURBAN" to GeoPosition(-31.5, 32.0),
        "AGULHAS" to GeoPosition(-36.0, 20.0),
        "CAPE" to GeoPosition(-34.8, 17.6),
        "SOUTH_ATLANTIC" to GeoPosition(-25.0, 8.0),
        "GULF_OF_GUINEA" to GeoPosition(-3.0, -6.0),
        "DAKAR" to GeoPosition(13.0, -19.5),
        "CANARIES_WEST" to GeoPosition(28.0, -18.5),
        "SAINT_VINCENT" to GeoPosition(36.7, -9.3),
        "PORTUGAL" to GeoPosition(38.5, -10.8),
        "FINISTERRE" to GeoPosition(43.5, -10.0),
        "USHANT" to GeoPosition(48.6, -6.0),
        "CELTIC_SEA" to GeoPosition(50.0, -7.0),
        "BRISTOL_CHANNEL" to GeoPosition(51.3, -4.5),
        "CHANNEL_WEST" to GeoPosition(49.8, -3.5),
        "CHANNEL_MIDDLE" to GeoPosition(50.3, -0.8),
        "DOVER" to GeoPosition(51.0, 1.6),
        "NORTH_SEA" to GeoPosition(51.8, 3.0),
        "GIBRALTAR_WEST" to GeoPosition(36.0, -7.0),
        "GIBRALTAR" to GeoPosition(35.95, -5.6),
        "ALBORAN" to GeoPosition(36.2, -3.0),
        "BALEARIC" to GeoPosition(38.3, 0.6),
        "SARDINIA_SOUTH" to GeoPosition(38.3, 8.5),
        "SICILY_CHANNEL" to GeoPosition(37.3, 11.6),
        "MALTA" to GeoPosition(35.6, 14.0),
        "IONIAN" to GeoPosition(37.0, 18.5),
        "OTRANTO" to GeoPosition(40.2, 18.9),
        "ADRIATIC" to GeoPosition(43.0, 15.5),
        "ADRIATIC_NORTH" to GeoPosition(44.5, 13.3),
        "AEGEAN_SOUTH" to GeoPosition(36.3, 23.5),
        "AEGEAN" to GeoPosition(38.8, 25.0),
        "DARDANELLES" to GeoPosition(40.0, 26.2),
        "MARMARA" to GeoPosition(40.7, 28.0),
        "BOSPHORUS" to GeoPosition(41.1, 29.05),
        "BLACK_SEA" to GeoPosition(42.5, 29.8),
        "AZORES" to GeoPosition(38.5, -30.0),
        "WEST_ATLANTIC" to GeoPosition(25.0, -60.0),
        "MONA_PASSAGE" to GeoPosition(18.2, -67.8),
        "CARIBBEAN" to GeoPosition(13.5, -75.0),
        "COLON" to GeoPosition(9.4, -79.9),
        "PANAMA_PACIFIC" to GeoPosition(7.5, -79.5),
        "EAST_PACIFIC" to GeoPosition(15.0, -105.0),
        "WEST_PACIFIC" to GeoPosition(33.0, 145.0),
        "PHILIPPINE_SEA" to GeoPosition(15.0, 130.0),
        "NEW_GUINEA_NORTH" to GeoPosition(2.0, 152.0),
        "SOLOMON_EAST" to GeoPosition(-11.0, 164.0),
        "CORAL_SEA" to GeoPosition(-14.0, 155.5),
        "TASMAN" to GeoPosition(-30.0, 155.0),
    )

    private val lanes = listOf(
        "NANGANG" to "YANGTZE", "SHANGHAI" to "YANGTZE", "HAITONG" to "YANGTZE", "YIZHENG" to "YANGTZE",
        "YANGTZE" to "EAST_CHINA_SEA", "YANGTZE" to "YELLOW_SEA",
        "YELLOW_SEA" to "GUNSAN", "YELLOW_SEA" to "PYEONGTAEK", "YELLOW_SEA" to "KOREA_STRAIT",
        "KOREA_STRAIT" to "MASAN", "KOREA_STRAIT" to "EAST_CHINA_SEA", "KOREA_STRAIT" to "JAPAN_SOUTH",
        "EAST_CHINA_SEA" to "JAPAN_SOUTH", "EAST_CHINA_SEA" to "TAIWAN_STRAIT", "EAST_CHINA_SEA" to "PHILIPPINE_SEA",
        "TAIWAN_STRAIT" to "SOUTH_CHINA_SEA_N", "SOUTH_CHINA_SEA_N" to "SOUTH_CHINA_SEA_S",
        "SOUTH_CHINA_SEA_S" to "SINGAPORE_STRAIT", "SINGAPORE_STRAIT" to "SINGAPORE", "SINGAPORE" to "MALACCA",
        "MALACCA" to "SUMATRA_NORTH", "SUMATRA_NORTH" to "INDIAN_OCEAN", "INDIAN_OCEAN" to "MASCARENES",
        "MASCARENES" to "MAURITIUS", "MASCARENES" to "MADAGASCAR_SOUTH", "MADAGASCAR_SOUTH" to "DURBAN",
        "DURBAN" to "AGULHAS", "AGULHAS" to "CAPE", "CAPE" to "SOUTH_ATLANTIC", "SOUTH_ATLANTIC" to "GULF_OF_GUINEA",
        "GULF_OF_GUINEA" to "DAKAR", "DAKAR" to "CANARIES_WEST", "CANARIES_WEST" to "LAS PALMAS",
        "CANARIES_WEST" to "CASABLANCA", "CANARIES_WEST" to "SAINT_VINCENT", "CASABLANCA" to "GIBRALTAR_WEST",
        "SAINT_VINCENT" to "GIBRALTAR_WEST", "SAINT_VINCENT" to "PORTUGAL", "PORTUGAL" to "FINISTERRE",
        "FINISTERRE" to "USHANT", "USHANT" to "CHANNEL_WEST", "USHANT" to "CELTIC_SEA",
        "CELTIC_SEA" to "BRISTOL_CHANNEL", "BRISTOL_CHANNEL" to "BRISTOL", "CHANNEL_WEST" to "CHANNEL_MIDDLE",
        "CHANNEL_MIDDLE" to "SOUTHAMPTON", "CHANNEL_MIDDLE" to "DOVER", "DOVER" to "ZEEBRUGGE", "DOVER" to "NORTH_SEA",
        "NORTH_SEA" to "ZEEBRUGGE", "NORTH_SEA" to "ROTTERDAM", "NORTH_SEA" to "AMSTERDAM",
        "GIBRALTAR_WEST" to "GIBRALTAR", "GIBRALTAR" to "ALBORAN", "ALBORAN" to "BALEARIC", "BALEARIC" to "BARCELONA",
        "BALEARIC" to "SARDINIA_SOUTH", "SARDINIA_SOUTH" to "SICILY_CHANNEL", "SICILY_CHANNEL" to "MALTA",
        "MALTA" to "MISURATA", "MALTA" to "IONIAN", "IONIAN" to "OTRANTO", "OTRANTO" to "ADRIATIC",
        "ADRIATIC" to "ADRIATIC_NORTH", "ADRIATIC_NORTH" to "KOPER", "IONIAN" to "AEGEAN_SOUTH",
        "AEGEAN_SOUTH" to "AEGEAN", "AEGEAN" to "DARDANELLES", "DARDANELLES" to "MARMARA", "MARMARA" to "DERINCE",
        "MARMARA" to "BOSPHORUS", "BOSPHORUS" to "BLACK_SEA", "BLACK_SEA" to "CONSTANTA",
        "AZORES" to "SAINT_VINCENT", "AZORES" to "USHANT", "AZORES" to "WEST_ATLANTIC", "WEST_ATLANTIC" to "MONA_PASSAGE",
        "MONA_PASSAGE" to "CARIBBEAN", "CARIBBEAN" to "COLON", "COLON" to "BALBOA", "BALBOA" to "PANAMA_PACIFIC",
        "PANAMA_PACIFIC" to "EAST_PACIFIC", "EAST_PACIFIC" to "CORINTO", "EAST_PACIFIC" to "WEST_PACIFIC",
        "WEST_PACIFIC" to "JAPAN_SOUTH", "PHILIPPINE_SEA" to "NEW_GUINEA_NORTH", "NEW_GUINEA_NORTH" to "SOLOMON_EAST",
        "SOLOMON_EAST" to "CORAL_SEA", "CORAL_SEA" to "TASMAN", "TASMAN" to "PORT KEMBLA",
    )

    private val nodes = ports + waypoints

    private val neighbours: Map<String, List<String>> = lanes
        .flatMap { (a, b) -> listOf(a to b, b to a) }
        .groupBy({ it.first }, { it.second })

    /**
     * The lane through the ports of [route], in order, ports included: the shortest way from each to the next. A port
     * it doesn't know is skipped.
     */
    fun lane(route: List<String>): List<GeoPosition> {
        val known = route.map { it.uppercase() }.filter { it in ports }
        if (known.size < 2) return known.mapNotNull(ports::get)
        val names = known.zipWithNext().fold(listOf(known.first())) { path, (from, to) -> path + shortest(from, to).drop(1) }
        return names.map(nodes::getValue)
    }

    /** Dijkstra over [lanes], weighted by great-circle distance; a straight line when no lane links them. */
    private fun shortest(from: String, to: String): List<String> {
        val distance = mutableMapOf(from to 0.0)
        val previous = mutableMapOf<String, String>()
        val queue = PriorityQueue<Pair<String, Double>>(compareBy { it.second }).apply { add(from to 0.0) }
        while (queue.isNotEmpty()) {
            val (node, d) = queue.poll()
            if (node == to) break
            if (d > distance.getValue(node)) continue
            neighbours[node].orEmpty().forEach { next ->
                val candidate = d + arc(nodes.getValue(node), nodes.getValue(next))
                if (candidate < (distance[next] ?: Double.MAX_VALUE)) {
                    distance[next] = candidate
                    previous[next] = node
                    queue.add(next to candidate)
                }
            }
        }
        if (to != from && to !in previous) return listOf(from, to)
        return generateSequence(to) { previous[it] }.toList().reversed()
    }

    private fun arc(a: GeoPosition, b: GeoPosition): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        return acos((sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(dLon)).coerceIn(-1.0, 1.0))
    }
}
