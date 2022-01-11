package util.kdtree

interface KdMetric {
    fun distance(p1: KdPoint, p2: KdPoint): Double
}