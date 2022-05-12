package ru.rofleksey.sunnyway.nav

import ru.rofleksey.sunnyway.bound.MapBoundFactory
import ru.rofleksey.sunnyway.graph.Graph
import ru.rofleksey.sunnyway.graph.GraphEdge
import ru.rofleksey.sunnyway.rest.types.NavigationEdge
import ru.rofleksey.sunnyway.util.FibonacciHeap
import java.util.*

class DistanceNavigator(private val graph: Graph, private val mapBoundFactory: MapBoundFactory) : Navigator {
    companion object {
        private const val AVOID_FACTOR = 1000
    }

    private val distance = Array(graph.vertexList.size) { 0.0 }
    private val prevEdge = Array<GraphEdge?>(graph.vertexList.size) { null }
    private val heap = FibonacciHeap<Int>()
    private val heapEntries = Array<FibonacciHeap.Entry<Int>?>(graph.vertexList.size) { null }

    private fun reset() {
        distance.fill(Double.POSITIVE_INFINITY)
        prevEdge.fill(null)
        heap.clear()
        heapEntries.fill(null)
    }

    private fun setDistance(id: Int, newDistance: Double) {
        distance[id] = newDistance
        val heapEntry = heapEntries[id]
        if (heapEntry == null) {
            heapEntries[id] = heap.enqueue(id, newDistance)
        } else {
            heap.decreaseKey(heapEntry, newDistance)
        }
    }

    private fun restorePath(toId: Int): List<NavigationEdge> {
        val result = LinkedList<NavigationEdge>()
        var cur = toId
        while (prevEdge[cur] != null) {
            val edge = prevEdge[cur]!!
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
            cur = edge.fromVertex.graphId
        }
        return result.reversed()
    }

    @Synchronized
    override fun navigate(req: NavigationRequest): List<NavigationEdge> {
        val mapBound = mapBoundFactory.create(graph.vertexList[req.fromId].point, graph.vertexList[req.toId].point)
        reset()
        setDistance(req.fromId, 0.0)
        while (!heap.isEmpty()) {
            val cur = heap.dequeueMin().value
            if (cur == req.toId) {
                return restorePath(req.toId)
            }
            for (edge in graph.vertexList[cur].getEdges()) {
                if (!mapBound.isInside(edge.toVertex.point)) {
                    continue
                }
                val newDistance = if (edge.avoid) {
                    distance[cur] + AVOID_FACTOR * edge.distance
                } else {
                    distance[cur] + edge.distance
                }
                if (newDistance < distance[edge.toVertex.graphId]) {
                    setDistance(edge.toVertex.graphId, newDistance)
                    prevEdge[edge.toVertex.graphId] = edge
                }
            }
        }
        return listOf()
    }
}