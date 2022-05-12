package ru.rofleksey.sunnyway.graph

import ru.rofleksey.sunnyway.rest.types.GeoPoint
import ru.rofleksey.sunnyway.util.csv.CsvEdge
import ru.rofleksey.sunnyway.util.kdtree.HaversineMetric
import ru.rofleksey.sunnyway.util.kdtree.KdEntry
import ru.rofleksey.sunnyway.util.kdtree.KdPoint
import ru.rofleksey.sunnyway.util.kdtree.KdTree

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
            direction = edge.direction,
            avoid = edge.avoid
        )
        startVertex.addEdge(startGraphEdge)

        val endGraphEdge = GraphEdge(
            graphId = edgeCounter++,
            fromVertex = endVertex,
            toVertex = startVertex,
            leftShadow = edge.leftShadow,
            rightShadow = edge.rightShadow,
            distance = edge.distance,
            direction = edge.direction,
            avoid = edge.avoid
        )
        endVertex.addEdge(endGraphEdge)
    }

    fun build(): Result {
        val convertedPoints = ArrayList<KdEntry>(vertexList.size)
        vertexList.forEach { vertex ->
            val newX = Math.toRadians(vertex.point.lat)
            val newY = Math.toRadians(vertex.point.lon)
            convertedPoints.add(KdEntry(vertex.graphId, KdPoint(newX, newY)))
        }
        val kdTree = KdTree.fromPoints(convertedPoints, HaversineMetric())
        return Result(Graph(vertexList, edgeCounter), kdTree)
    }

    data class Result(val graph: Graph, val kdTree: KdTree)
}