package ru.rofleksey.sunnyway.rest.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.rofleksey.sunnyway.graph.GraphEdge
import ru.rofleksey.sunnyway.rest.component.GraphComponent
import ru.rofleksey.sunnyway.rest.types.EdgeWithCost
import ru.rofleksey.sunnyway.rest.types.GeoPoint
import ru.rofleksey.sunnyway.util.sun.Sun
import ru.rofleksey.sunnyway.util.sun.SunCostCalculator
import java.util.*

@Service
class ShadowMapService(private val graph: GraphComponent) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(ShadowMapService::class.java)
    }

    private val edgeCostCalculator = SunCostCalculator(graph.graph)

    fun getShadowMap(center: GeoPoint, radius: Double): List<EdgeWithCost> {
        val startTime = System.currentTimeMillis()
        val vertices = graph.locateAll(center, radius)
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
        log.debug("Build shadow map with {} edges in {} ms", result.size, System.currentTimeMillis() - startTime)
        return result
    }
}