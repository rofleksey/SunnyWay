package nav

import bound.MapBoundFactory
import graph.Graph
import graph.GraphEdge
import rest.NavigationEdge
import util.FibonacciHeap
import util.Util.Companion.PEDESTRIAN_SPEED
import util.sun.Sun
import util.sun.SunCostCalculator
import util.sun.SunResult
import java.util.*
import kotlin.collections.HashMap

class DiscreteShadowNavigator(
    private val graph: Graph,
    private val mapBoundFactory: MapBoundFactory,
) : Navigator {
    private val totalCost = Array(graph.vertexList.size) { 0.0 }
    private val prevEdge = Array<GraphEdge?>(graph.vertexList.size) { null }
    private val heap = FibonacciHeap<Int>()
    private val edgeCostCalculator = SunCostCalculator(graph)
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

    private fun restorePath(toId: Int, sunResult: SunResult, isSunUp: Boolean): List<NavigationEdge> {
        val result = LinkedList<NavigationEdge>()
        var curVertexId = toId
        while (prevEdge[curVertexId] != null) {
            val edge = prevEdge[curVertexId]!!
            val metadata = HashMap<String, Any?>().apply {
                if (isSunUp) {
                    put("azimuth", Math.toDegrees(sunResult.azimuth))
                    put("costFactor", edgeCostCalculator.getComputedFactorById(edge.graphId))
                    put("buildingsLeft", edge.leftShadow)
                    put("buildingsRight", edge.rightShadow)
                    put("elevation", sunResult.elevation)
                } else {
                    put("azimuth", null)
                }
            }
            val time = (1000.0 * edge.distance / PEDESTRIAN_SPEED).toLong()
            result.add(
                NavigationEdge(
                    edge.fromVertex.point,
                    edge.toVertex.point,
                    edge.graphId,
                    edge.toVertex.graphId,
                    edge.distance,
                    time,
                    edgeCostCalculator.getComputedCostById(edge.graphId, edge.distance),
                    metadata
                )
            )
            curVertexId = edge.fromVertex.graphId
        }
        return result.reversed()
    }

    private fun navigateStatic(fromId: Int, toId: Int, preferShadow: Boolean, curSunTime: Long): List<NavigationEdge> {
        val mapBound = mapBoundFactory.create(graph.vertexList[fromId].point, graph.vertexList[toId].point)
        reset()
        setTotalCost(fromId, 0.0)
        val calendar = Calendar.getInstance()
        val sunResult = Sun(graph.vertexList[toId].point.lat, graph.vertexList[toId].point.lon, 3).run {
            setTime(calendar.apply {
                timeInMillis = curSunTime
            })
            calculate()
        }
        edgeCostCalculator.reset(sunResult, preferShadow)
        val isSunUp = edgeCostCalculator.isSunUp()
        while (!heap.isEmpty()) {
            val cur = heap.dequeueMin().value
            if (cur == toId) {
                return restorePath(toId, sunResult, isSunUp)
            }
            for (edge in graph.vertexList[cur].getEdges()) {
                if (!mapBound.isInside(edge.toVertex.point)) {
                    continue
                }
                val edgeCost = if (isSunUp) {
                    edgeCostCalculator.getCost(edge)
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
        if (req.timeSampling < 0) {
            return navigateStatic(curFromId, req.toId, req.preferShadow, curTime)
        }
        val fullPath = ArrayList<NavigationEdge>()
        var iterations = 0
        while (true) {
            val tempPath = navigateStatic(curFromId, req.toId, req.preferShadow, curTime)
            if (tempPath.isEmpty()) {
                return listOf()
            }
            val (newFromId, newCurTime) = getNewStartId(tempPath, curTime, curTime + req.timeSampling)
            tempPath.first { edge ->
                fullPath.add(edge)
                edge.toVertexId == newFromId
            }
            if (newFromId == req.toId) {
                println("[${req.timeSampling}] done in $iterations iterations")
                return fullPath
            }
            curTime = newCurTime
            curFromId = newFromId
            iterations++
        }
    }
}