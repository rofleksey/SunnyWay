package ru.rofleksey.sunnyway.util.kdtree

import kotlin.math.*

class HaversineMetric : KdMetric {
    override fun distance(p1: KdPoint, p2: KdPoint): Double {
        return 2 * asin(sqrt(sin(0.5 * p1.dx(p2)).pow(2) + cos(p1.x) * cos(p2.x) * sin(0.5 * p1.dy(p2)).pow(2)))
    }
}