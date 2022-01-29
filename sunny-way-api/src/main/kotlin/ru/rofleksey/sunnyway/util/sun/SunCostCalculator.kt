package ru.rofleksey.sunnyway.util.sun

import ru.rofleksey.sunnyway.graph.Graph
import ru.rofleksey.sunnyway.graph.GraphEdge
import ru.rofleksey.sunnyway.util.Util
import kotlin.math.min

class SunCostCalculator(graph: Graph) {
    companion object {
        private const val MAX_FACTOR = 10.0
        private const val MIN_ANGLE = 5.0
        private const val MIN_FACTOR = 1.0
        private const val MAX_ANGLE = 50.0
        private const val MIN_ELEVATION = 1.0
    }
    private val edgeCostFactorArray = Array(graph.edgeCount) { -1.0 }
    private var sunResult: SunResult? = null
    private var preferShadow = true

    fun isSunUp() = sunResult!!.elevation >= MIN_ELEVATION

    fun getComputedFactorById(edgeId: Int) = edgeCostFactorArray[edgeId]

    fun getFactor(edge: GraphEdge): Double {
        if (edgeCostFactorArray[edge.graphId] < 0) {
            val azimuthDegrees = Util.convert360to180(Math.toDegrees(sunResult!!.azimuth))
            val sunAngle = Util.findOrtAngle(edge.direction, azimuthDegrees)
            val raySide = Util.getRaySide(edge.direction, Util.invertAngle180(azimuthDegrees))
            if (raySide == 0.0 || sunAngle <= MIN_ANGLE) {
                if (preferShadow) {
                    edgeCostFactorArray[edge.graphId] = MAX_FACTOR
                } else {
                    edgeCostFactorArray[edge.graphId] = MIN_FACTOR
                }
            } else {
                val buildingsFactor = if (raySide > 0) edge.leftShadow else edge.rightShadow
                val sunMultiplier = min((sunAngle - MIN_ANGLE) / (MAX_ANGLE - MIN_ANGLE), 1.0)
                if (preferShadow) {
                    val shadowFactor = MAX_FACTOR + (MIN_FACTOR - MAX_FACTOR) * sunMultiplier
                    edgeCostFactorArray[edge.graphId] =
                        buildingsFactor * shadowFactor + (1 - buildingsFactor) * MAX_FACTOR
                } else {
                    val shadowFactor = MIN_FACTOR + (MAX_FACTOR - MIN_FACTOR) * sunMultiplier
                    edgeCostFactorArray[edge.graphId] =
                        buildingsFactor * shadowFactor + (1 - buildingsFactor) * MIN_FACTOR
                }
            }
        }
        return edgeCostFactorArray[edge.graphId]
    }

    fun getCost(edge: GraphEdge) = getFactor(edge) * edge.distance
    fun getComputedCostById(edgeId: Int, edgeDistance: Double) = edgeCostFactorArray[edgeId] * edgeDistance

    fun reset(sunResult: SunResult, preferShadow: Boolean) {
        this.sunResult = sunResult
        this.preferShadow = preferShadow
        edgeCostFactorArray.fill(-1.0)
    }
}