package rest.service

import graph.Graph
import rest.GeoPoint
import kotlin.math.max
import kotlin.math.min

class ServiceAreaService(private val graph: Graph) {
    private var serviceAreaResponseCached: List<GeoPoint>? = null

    fun getServiceArea(): List<GeoPoint> {
        if (serviceAreaResponseCached == null) {
            var minLat = Double.POSITIVE_INFINITY
            var maxLat = Double.NEGATIVE_INFINITY
            var minLon = Double.POSITIVE_INFINITY
            var maxLon = Double.NEGATIVE_INFINITY
            for (point in graph.vertexList) {
                minLat = min(minLat, point.point.lat)
                maxLat = max(maxLat, point.point.lat)
                minLon = min(minLon, point.point.lon)
                maxLon = max(maxLon, point.point.lon)
            }
            serviceAreaResponseCached = listOf(
                GeoPoint(minLat, minLon),
                GeoPoint(minLat, maxLon),
                GeoPoint(maxLat, maxLon),
                GeoPoint(maxLat, minLon),
                GeoPoint(minLat, minLon)
            )
        }
        return serviceAreaResponseCached!!
    }
}