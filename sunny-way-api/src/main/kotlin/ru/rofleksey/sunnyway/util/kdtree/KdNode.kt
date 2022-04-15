package ru.rofleksey.sunnyway.util.kdtree

class KdNode(
    val id: Int,
    val point: KdPoint,
    var left: KdNode?,
    var right: KdNode?,
    val vertical: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KdNode

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}