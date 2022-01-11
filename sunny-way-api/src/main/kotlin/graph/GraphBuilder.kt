package graph

import util.csv.CsvEdge
import rest.GeoPoint

class GraphBuilder(edgeCount: Int) {
    private val vertexMap = HashMap<GeoPoint, GraphVertex>(edgeCount)
    private val vertexList = ArrayList<GraphVertex>(edgeCount)

    private var vertexCounter = 0
    private var edgeCounter = 0

    private fun addVertex(point: GeoPoint): GraphVertex {
        val newVertex = GraphVertex(vertexCounter++, point)
        vertexMap[point] = newVertex
        vertexList.add(newVertex)
        return newVertex
    }

    fun addEdge(edge: CsvEdge) {
        val startVertex = if (!vertexMap.containsKey(edge.start)) {
            addVertex(edge.start)
        } else {
            vertexMap[edge.start]!!
        }

        val endVertex = if (!vertexMap.containsKey(edge.end)) {
            addVertex(edge.end)
        } else {
            vertexMap[edge.end]!!
        }

        val startGraphEdge = GraphEdge(
            graphId = edgeCounter++,
            fromVertex = startVertex,
            toVertex = endVertex,
            leftShadow = edge.leftShadow,
            rightShadow = edge.rightShadow,
            distance = edge.distance,
            direction = edge.direction
        )
        startVertex.addEdge(startGraphEdge)

        val endGraphEdge = GraphEdge(
            graphId = edgeCounter++,
            fromVertex = endVertex,
            toVertex = startVertex,
            leftShadow = edge.leftShadow,
            rightShadow = edge.rightShadow,
            distance = edge.distance,
            direction = edge.direction
        )
        endVertex.addEdge(endGraphEdge)
    }

    fun build(): Result {
        val vertexLocator = VertexLocator.fromPoints(vertexList)
        return Result(Graph(vertexList, edgeCounter), vertexLocator)
    }

    data class Result(val graph: Graph, val vertexLocator: VertexLocator)
}