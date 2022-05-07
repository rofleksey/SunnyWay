package ru.rofleksey.sunnyway.util.sun

import ru.rofleksey.sunnyway.graph.Graph
import ru.rofleksey.sunnyway.graph.GraphEdge

class SunCostCache(graph: Graph) {
    private val edgeCostFactorArray = Array(graph.edgeCount) { -1.0 }
    private var sunResult: SunResult? = null
    private var preferShadow = true
    private var maxFactor = SunResult.DEFAULT_MAX_FACTOR

    fun getComputedFactorById(edgeId: Int) = edgeCostFactorArray[edgeId]

    fun getFactor(edge: GraphEdge): Double {
        if (edgeCostFactorArray[edge.graphId] < 0) {
            edgeCostFactorArray[edge.graphId] = sunResult!!.calculateFactor(
                edge.direction, edge.leftShadow,
                edge.rightShadow, preferShadow, maxFactor
            )
        }
        return edgeCostFactorArray[edge.graphId]
    }

    fun getCost(edge: GraphEdge) = getFactor(edge) * edge.distance
    fun getComputedCostById(edgeId: Int, edgeDistance: Double) = edgeCostFactorArray[edgeId] * edgeDistance

    fun reset(sunResult: SunResult, maxFactor: Double = SunResult.DEFAULT_MAX_FACTOR, preferShadow: Boolean = true) {
        this.sunResult = sunResult
        this.preferShadow = preferShadow
        this.maxFactor = maxFactor
        edgeCostFactorArray.fill(-1.0)
    }
}