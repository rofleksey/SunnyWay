package graph

data class GraphEdge(
    val graphId: Int,
    val fromVertex: GraphVertex,
    val toVertex: GraphVertex,
    val leftShadow: Double,
    val rightShadow: Double,
    val distance: Double,
    val direction: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphEdge

        if (fromVertex == other.fromVertex && toVertex == other.toVertex) {
            return true
        }

        if (fromVertex == other.toVertex && toVertex == other.fromVertex) {
            return true
        }

        return false
    }

    override fun hashCode(): Int {
        return fromVertex.graphId + toVertex.graphId
    }
}