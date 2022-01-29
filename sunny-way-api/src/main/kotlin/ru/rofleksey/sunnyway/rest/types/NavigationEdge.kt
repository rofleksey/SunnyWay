package ru.rofleksey.sunnyway.rest.types

data class NavigationEdge (
    val fromPoint: GeoPoint,
    val toPoint: GeoPoint,
    val edgeId: Int,
    val toVertexId: Int,
    val distance: Double,
    val time: Long,
    val cost: Double,
    val metadata: Map<String, Any?>
)