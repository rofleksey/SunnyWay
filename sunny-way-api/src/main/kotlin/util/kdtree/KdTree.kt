package util.kdtree

class KdTree private constructor(private val metric: KdMetric) {
    companion object {
        fun fromPoints(points: List<KdEntry>, metric: KdMetric): KdTree {
            val sorted = points.sortedBy { it.point }
            val tree = KdTree(metric)
            sorted.forEach { entry ->
                tree.insert(entry)
            }
            return tree
        }
    }
    private var root: KdNode? = null

    private fun insert(node: KdNode?, entry: KdEntry, vertical: Boolean): KdNode {
        if (node == null) {
            return KdNode(entry.id, entry.point, null, null, vertical)
        }
        if (node.point.x == entry.point.x && node.point.y == entry.point.y) {
            return node
        }
        if (node.vertical && entry.point.x < node.point.x || !node.vertical && entry.point.y < node.point.y) {
            node.left = insert(node.left, entry, !node.vertical)
        } else {
            node.right = insert(node.right, entry, !node.vertical)
        }
        return node
    }

    private fun insert(entry: KdEntry) {
        root = insert(root, entry, true)
    }

    data class KdNearestData(var closest: KdNode?, var minDistance: Double)

    private fun nearest(
        node: KdNode?,
        point: KdPoint,
        requestData: KdNearestData
    ) {
        if (node == null) {
            return
        }
        val dist = metric.distance(point, node.point)
        if (dist < requestData.minDistance) {
            requestData.minDistance = dist
            requestData.closest = node
        }
        if (node.vertical && point.x < node.point.x || !node.vertical && point.y < node.point.y) {
            nearest(node.left, point, requestData)
            if (node.vertical && point.x + requestData.minDistance >= node.point.x || !node.vertical && point.y + requestData.minDistance >= node.point.y) {
                nearest(node.right, point, requestData)
            }
        } else {
            nearest(node.right, point, requestData)
            if (node.vertical && point.x - requestData.minDistance <= node.point.x || !node.vertical && point.y - requestData.minDistance <= node.point.y) {
                nearest(node.left, point, requestData)
            }
        }
    }

    fun nearest(point: KdPoint, maxDistance: Double): Int? {
        val requestData = KdNearestData(null, maxDistance)
        nearest(root, point, requestData)
        return requestData.closest?.id
    }

    private fun nearestAll(
        node: KdNode?,
        point: KdPoint,
        distance: Double,
        result: MutableList<Int>,
    ) {
        if (node == null) {
            return
        }
        val dist = metric.distance(point, node.point)
        if (dist <= distance) {
            // println("dist = $dist, distance = $distance")
            result.add(node.id)
        }
        if (node.vertical && point.x < node.point.x || !node.vertical && point.y < node.point.y) {
            nearestAll(node.left, point, distance, result)
            if (node.vertical && point.x + distance >= node.point.x || !node.vertical && point.y + distance >= node.point.y) {
                nearestAll(node.right, point, distance, result)
            }
        } else {
            nearestAll(node.right, point, distance, result)
            if (node.vertical && point.x - distance <= node.point.x || !node.vertical && point.y - distance <= node.point.y) {
                nearestAll(node.left, point, distance, result)
            }
        }
    }

    fun nearestAll(point: KdPoint, distance: Double): List<Int> {
        val result = ArrayList<Int>()
        nearestAll(root, point, distance, result)
        return result
    }
}