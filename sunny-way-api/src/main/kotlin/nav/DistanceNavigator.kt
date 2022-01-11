package nav

import bound.MapBoundFactory
import graph.Graph
import graph.GraphEdge
import rest.NavigationEdge
import rest.NavigationResult
import util.FibonacciHeap
import util.Util.Companion.PEDESTRIAN_SPEED
import java.util.*

class DistanceNavigator(private val graph: Graph, private val mapBoundFactory: MapBoundFactory) : Navigator {
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

    private fun restorePath(toId: Int, graph: Graph): List<NavigationEdge> {
        val result = LinkedList<NavigationEdge>()
        var cur = toId
        while (prevEdge[cur] != null) {
            val edge = prevEdge[cur]!!
            val time = (1000.0 * edge.distance / PEDESTRIAN_SPEED).toLong()
            result.add(
                NavigationEdge(
                    edge.fromVertex.point,
                    edge.toVertex.point,
                    edge.graphId,
                    edge.toVertex.graphId,
                    edge.distance,
                    time,
                    -1.0,
                    mapOf()
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
                return restorePath(req.toId, graph)
            }
            for (edge in graph.vertexList[cur].getEdges()) {
                if (!mapBound.isInside(edge.toVertex.point)) {
                    continue
                }
                val newDistance = distance[cur] + edge.distance
                if (newDistance < distance[edge.toVertex.graphId]) {
                    setDistance(edge.toVertex.graphId, newDistance)
                    prevEdge[edge.toVertex.graphId] = edge
                }
            }
        }
        return listOf()
    }
}