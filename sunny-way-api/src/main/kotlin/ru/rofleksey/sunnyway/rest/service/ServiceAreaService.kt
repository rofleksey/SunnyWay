package ru.rofleksey.sunnyway.rest.service

import org.springframework.stereotype.Service
import ru.rofleksey.sunnyway.rest.component.GraphComponent
import ru.rofleksey.sunnyway.rest.types.GeoPoint
import kotlin.math.max
import kotlin.math.min

@Service
class ServiceAreaService(graph: GraphComponent) {
    private val serviceAreaResponseCached: List<GeoPoint>
    private val centerCached: GeoPoint

    init {
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

        centerCached = GeoPoint((minLat + maxLat / 2), (minLon + maxLon) / 2)
    }

    fun getServiceArea(): List<GeoPoint> = serviceAreaResponseCached
    fun getCenter(): GeoPoint = centerCached
}