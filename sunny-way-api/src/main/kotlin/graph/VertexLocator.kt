package graph

import rest.GeoPoint
import util.Util.Companion.EARTH_RADIUS_M
import util.kdtree.HaversineMetric
import util.kdtree.KdEntry
import util.kdtree.KdPoint
import util.kdtree.KdTree

class VertexLocator private constructor(private val kdTree: KdTree) {
    companion object {
        fun fromPoints(points: List<GraphVertex>): VertexLocator {
            val convertedPoints = ArrayList<KdEntry>(points.size)
            points.forEach { vertex ->
                val newX = Math.toRadians(vertex.point.lat)
                val newY = Math.toRadians(vertex.point.lon)
                convertedPoints.add(KdEntry(vertex.graphId, KdPoint(newX, newY)))
            }
            return VertexLocator(KdTree.fromPoints(convertedPoints, HaversineMetric()))
        }
    }

    fun locate(point: GeoPoint, maxDistance: Double): Int? {
        val newX = Math.toRadians(point.lat)
        val newY = Math.toRadians(point.lon)
        val newDist = maxDistance / EARTH_RADIUS_M
        return kdTree.nearest(KdPoint(newX, newY), newDist)
    }

    fun locateAll(point: GeoPoint, distance: Double): List<Int> {
        val newX = Math.toRadians(point.lat)
        val newY = Math.toRadians(point.lon)
        val newDist = distance / EARTH_RADIUS_M
        return kdTree.nearestAll(KdPoint(newX, newY), newDist)
    }
}