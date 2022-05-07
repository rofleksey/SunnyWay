package ru.rofleksey.sunnyway.nav

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.rofleksey.sunnyway.bound.MapBoundFactory
import ru.rofleksey.sunnyway.graph.Graph
import ru.rofleksey.sunnyway.graph.GraphEdge
import ru.rofleksey.sunnyway.rest.types.NavigationEdge
import ru.rofleksey.sunnyway.util.FibonacciHeap
import ru.rofleksey.sunnyway.util.Util.Companion.PEDESTRIAN_SPEED
import ru.rofleksey.sunnyway.util.sun.Sun
import ru.rofleksey.sunnyway.util.sun.SunCostCache
import java.util.*

class DiscreteShadowNavigator(
    private val graph: Graph,
    private val mapBoundFactory: MapBoundFactory,
) : Navigator {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(DiscreteShadowNavigator::class.java)
        private const val TIME_SAMPLING = 15 * 60 * 1000
    }

    private val totalCost = Array(graph.vertexList.size) { 0.0 }
    private val prevEdge = Array<GraphEdge?>(graph.vertexList.size) { null }
    private val heap = FibonacciHeap<Int>()
    private val sunCostCache = SunCostCache(graph)
    private val heapEntries = Array<FibonacciHeap.Entry<Int>?>(graph.vertexList.size) { null }

    private fun reset() {
        totalCost.fill(Double.POSITIVE_INFINITY)
        prevEdge.fill(null)
        heap.clear()
        heapEntries.fill(null)
    }

    private fun setTotalCost(id: Int, newDistance: Double) {
        totalCost[id] = newDistance
        val heapEntry = heapEntries[id]
        if (heapEntry == null) {
            heapEntries[id] = heap.enqueue(id, newDistance)
        } else {
            heap.decreaseKey(heapEntry, newDistance)
        }
    }

    private fun restorePath(toId: Int): List<NavigationEdge> {
        val result = LinkedList<NavigationEdge>()
        var curVertexId = toId
        while (prevEdge[curVertexId] != null) {
            val edge = prevEdge[curVertexId]!!
            result.add(
                NavigationEdge(
                    edge.fromVertex.point,
                    edge.toVertex.point,
                    edge.graphId,
                    edge.toVertex.graphId,
                    edge.direction,
                    edge.leftShadow,
                    edge.rightShadow,
                    edge.distance
                )
            )
            curVertexId = edge.fromVertex.graphId
        }
        return result.reversed()
    }

    private fun navigateStatic(
        fromId: Int,
        toId: Int,
        maxFactor: Double,
        preferShadow: Boolean,
        curSunTime: Long
    ): List<NavigationEdge> {
        val mapBound = mapBoundFactory.create(graph.vertexList[fromId].point, graph.vertexList[toId].point)
        reset()
        setTotalCost(fromId, 0.0)
        val calendar = Calendar.getInstance()
        val sunResult = Sun(graph.vertexList[toId].point.lat, graph.vertexList[toId].point.lon, 1).run {
            setTime(calendar.apply {
                timeInMillis = curSunTime
            })
            calculate()
        }
        sunCostCache.reset(sunResult, maxFactor = maxFactor, preferShadow = preferShadow)
        val isSunUp = sunResult.isSunUp()
        while (!heap.isEmpty()) {
            val cur = heap.dequeueMin().value
            if (cur == toId) {
                return restorePath(toId)
            }
            for (edge in graph.vertexList[cur].getEdges()) {
                if (!mapBound.isInside(edge.toVertex.point)) {
                    continue
                }
                val edgeCost = if (isSunUp) {
                    sunCostCache.getCost(edge)
                } else {
                    edge.distance
                }
                val newCost = totalCost[cur] + edgeCost
                if (newCost < totalCost[edge.toVertex.graphId]) {
                    setTotalCost(edge.toVertex.graphId, newCost)
                    prevEdge[edge.toVertex.graphId] = edge
                }
            }
        }
        return listOf()
    }

    private data class PartialPathResult(val newFromId: Int, val newCurTime: Long)

    private fun getNewStartId(path: List<NavigationEdge>, startTime: Long, endTime: Long): PartialPathResult {
        var curEndTime = startTime
        path.forEach { edge ->
            val passTime = (1000.0 * edge.distance / PEDESTRIAN_SPEED).toLong()
            if (curEndTime + passTime > endTime) {
                return PartialPathResult(edge.toVertexId, curEndTime + passTime)
            }
            curEndTime += passTime
        }
        return PartialPathResult(path.last().toVertexId, curEndTime)
    }

    @Synchronized
    override fun navigate(req: NavigationRequest): List<NavigationEdge> {
        var curTime = req.curTime
        var curFromId = req.fromId
        val fullPath = ArrayList<NavigationEdge>()
        var iterations = 0
        while (true) {
            val tempPath = navigateStatic(curFromId, req.toId, req.maxFactor, req.preferShadow, curTime)
            if (tempPath.isEmpty()) {
                return listOf()
            }
            val (newFromId, newCurTime) = getNewStartId(tempPath, curTime, curTime + TIME_SAMPLING)
            tempPath.first { edge ->
                fullPath.add(edge)
                edge.toVertexId == newFromId
            }
            if (newFromId == req.toId) {
                log.debug("Done in {} iterations", iterations)
                return fullPath
            }
            curTime = newCurTime
            curFromId = newFromId
            iterations++
        }
    }
}