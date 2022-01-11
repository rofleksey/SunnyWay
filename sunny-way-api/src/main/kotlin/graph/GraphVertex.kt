package graph

import rest.GeoPoint

data class GraphVertex(val graphId: Int, val point: GeoPoint) {
    private val edges = ArrayList<GraphEdge>()

    fun addEdge(edge: GraphEdge) {
        edges.add(edge)
    }

    fun getEdges(): List<GraphEdge> {
        return edges
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphVertex

        if (graphId != other.graphId) return false

        return true
    }

    override fun hashCode(): Int {
        return graphId
    }
}