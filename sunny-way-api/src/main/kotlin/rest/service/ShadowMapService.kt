package rest.service

import graph.Graph
import graph.GraphEdge
import graph.VertexLocator
import rest.EdgeWithCost
import rest.GeoPoint
import util.sun.Sun
import util.sun.SunCostCalculator
import java.util.*
import kotlin.collections.HashSet

class ShadowMapService(private val graph: Graph, private val vertexLocator: VertexLocator) {
    private val edgeCostCalculator = SunCostCalculator(graph)

    fun getShadowMap(center: GeoPoint, radius: Double): List<EdgeWithCost> {
        val startTime = System.currentTimeMillis()
        val vertices = vertexLocator.locateAll(center, radius)
        val edgesSet = HashSet<GraphEdge>()
        vertices.forEach { vertexId ->
            val vertex = graph.vertexList[vertexId]
            edgesSet.addAll(vertex.getEdges())
        }
        val calendar = Calendar.getInstance()
        val sunResult = Sun(center.lat, center.lon, 3).run {
            setTime(calendar.apply {
                timeInMillis = 1641549609L * 1000L // 07.01.2022 13:00
            })
            calculate()
        }
        edgeCostCalculator.reset(sunResult, true)
        val result = edgesSet.map { graphEdge ->
            if (edgeCostCalculator.isSunUp()) {
                EdgeWithCost(graphEdge.fromVertex.point, graphEdge.toVertex.point, edgeCostCalculator.getFactor(graphEdge))
            } else {
                EdgeWithCost(graphEdge.fromVertex.point, graphEdge.toVertex.point, 1.0)
            }
        }
        val computeTime = System.currentTimeMillis() - startTime
        println("Build shadow map with ${result.size} edges in $computeTime ms")
        return result
    }
}