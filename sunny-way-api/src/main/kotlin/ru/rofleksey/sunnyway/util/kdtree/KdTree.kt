package ru.rofleksey.sunnyway.util.kdtree

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class KdTree private constructor(private val metric: KdMetric) {
    companion object {
        private fun fromPointsImpl(
            tree: KdTree,
            points: MutableList<KdEntry>,
            parent: KdNode?,
            isVertical: Boolean,
            fromIndex: Int,
            toIndex: Int
        ) {
            if (toIndex - fromIndex == 0) {
                return
            }
            if (toIndex - fromIndex == 1) {
                tree.insert(points[fromIndex], parent)
                return
            }
            if (isVertical) {
                points.subList(fromIndex, toIndex).sortBy { it.point.x }
            } else {
                points.subList(fromIndex, toIndex).sortBy { it.point.y }
            }
            var mid = fromIndex + (toIndex - fromIndex) / 2
            val medianToFind = if (isVertical) {
                points[mid].point.x
            } else {
                points[mid].point.y
            }
            while (mid > fromIndex) {
                if (isVertical && points[mid - 1].point.x >= medianToFind
                    || !isVertical && points[mid - 1].point.y >= medianToFind
                ) {
                    mid--
                    continue
                }
                break
            }
            val newParent = tree.insert(points[mid], parent)
            fromPointsImpl(tree, points, newParent, !isVertical, fromIndex, mid)
            fromPointsImpl(tree, points, newParent, !isVertical, mid + 1, toIndex)
        }

        fun fromPoints(points: List<KdEntry>, metric: KdMetric): KdTree {
            val tree = KdTree(metric)
            val time = System.nanoTime()
            fromPointsImpl(tree, ArrayList(points), null, true, 0, points.size)
            val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time)
            log.info("Built tree in {} ms", elapsed)
            return tree
        }

        private val log: Logger = LoggerFactory.getLogger(KdTree::class.java)
    }

    private var root: KdNode? = null
    var size = 0

    private fun insertImpl(node: KdNode?, entry: KdEntry, vertical: Boolean, data: KdInsertData): KdNode {
        if (node == null) {
            val newNode = KdNode(entry.id, entry.point, null, null, vertical)
            data.newNode = newNode
            return newNode
        }
        if (node.id == entry.id) {
            return node
        }
        if (node.vertical && entry.point.x < node.point.x || !node.vertical && entry.point.y < node.point.y) {
            node.left = insertImpl(node.left, entry, !node.vertical, data)
        } else {
            node.right = insertImpl(node.right, entry, !node.vertical, data)
        }
        return node
    }

    data class KdInsertData(var newNode: KdNode?)

    private fun insert(entry: KdEntry, node: KdNode? = null): KdNode? {
        val insertData = KdInsertData(null)
        if (node == null) {
            root = insertImpl(root, entry, true, insertData)
        } else {
            insertImpl(node, entry, node.vertical, insertData)
        }
        size++
        return insertData.newNode
    }

    data class KdNearestData(var closest: KdNode?, var minDistance: Double, var visitedNodes: Int)

    private fun recalculateDistance(
        node: KdNode,
        target: KdPoint,
        data: KdNearestData
    ) {
        val dist = metric.distance(target, node.point)
        if (dist < data.minDistance) {
            data.minDistance = dist
            data.closest = node
        }
    }

    private fun nearestImpl(
        node: KdNode?,
        target: KdPoint,
        requestData: KdNearestData
    ) {
        if (node == null) {
            return
        }
        requestData.visitedNodes++
        if (node.vertical && target.x < node.point.x || !node.vertical && target.y < node.point.y) {
            nearestImpl(node.left, target, requestData)
            recalculateDistance(node, target, requestData)
            if (node.vertical && target.x + requestData.minDistance >= node.point.x || !node.vertical && target.y + requestData.minDistance >= node.point.y) {
                nearestImpl(node.right, target, requestData)
            }
        } else {
            nearestImpl(node.right, target, requestData)
            recalculateDistance(node, target, requestData)
            if (node.vertical && target.x - requestData.minDistance <= node.point.x || !node.vertical && target.y - requestData.minDistance <= node.point.y) {
                nearestImpl(node.left, target, requestData)
            }
        }
    }

    fun nearest(point: KdPoint, maxDistance: Double): KdNearestData {
        val requestData = KdNearestData(null, maxDistance, 0)
        nearestImpl(root, point, requestData)
        log.debug("visited {}/{} tree nodes", requestData.visitedNodes, size)
        return requestData
    }

    private fun allNearestImpl(
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
            result.add(node.id)
        }
        if (node.vertical && point.x - distance <= node.point.x || !node.vertical && point.y - distance <= node.point.y) {
            allNearestImpl(node.left, point, distance, result)
        }
        if (node.vertical && point.x + distance >= node.point.x || !node.vertical && point.y + distance >= node.point.y) {
            allNearestImpl(node.right, point, distance, result)
        }
    }

    fun allNearest(point: KdPoint, distance: Double): List<Int> {
        val result = ArrayList<Int>()
        allNearestImpl(root, point, distance, result)
        return result
    }
}