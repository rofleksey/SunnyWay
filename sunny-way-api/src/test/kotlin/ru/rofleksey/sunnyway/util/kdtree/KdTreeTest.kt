package ru.rofleksey.sunnyway.util.kdtree

import org.junit.jupiter.api.Test
import ru.rofleksey.sunnyway.graph.GraphBuilder
import ru.rofleksey.sunnyway.rest.types.GeoPoint
import ru.rofleksey.sunnyway.util.Util
import ru.rofleksey.sunnyway.util.csv.CsvEdge
import ru.rofleksey.sunnyway.util.csv.CsvReader
import java.io.File
import java.io.FileReader
import java.nio.charset.Charset
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertTrue

class KdTreeTest {
    @Test
    fun averageTimeTest() {
        val random = Random(1337)
        val file = File("data", "edges.csv")
        val edges = CsvReader(FileReader(file, Charset.forName("UTF-8"))).use { reader ->
            reader.readHeader()
            reader.sequence().map { row ->
                val start = GeoPoint(row["start_lat"]!!.toDouble(), row["start_lon"]!!.toDouble())
                val end = GeoPoint(row["end_lat"]!!.toDouble(), row["end_lon"]!!.toDouble())
                CsvEdge(
                    start = start,
                    end = end,
                    leftShadow = row["left_shadow"]!!.toDouble(),
                    rightShadow = row["right_shadow"]!!.toDouble(),
                    distance = row["distance"]!!.toDouble(),
                    direction = row["direction"]!!.toDouble(),
                )
            }.toList()
        }
        System.gc()
        val builder = GraphBuilder(edges.size)
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = Double.MIN_VALUE
        edges.forEach { edge ->
            listOf(edge.start, edge.end).forEach { point ->
                minLat = min(minLat, point.lat)
                minLon = min(minLon, point.lon)
                maxLat = max(maxLat, point.lat)
                maxLon = max(maxLon, point.lon)
            }
            builder.addEdge(edge)
        }
        val (_, kdTree) = builder.build()
        var totalTime = 0L
        var totalVisited = 0L
        var maxTime = 0L
        var maxVisited = 0
        var minVisited = Integer.MAX_VALUE
        var successfulCount = 0
        val repeatCount = 100000
        repeat(repeatCount) {
            val lat = minLat + (maxLat - minLat) * random.nextDouble()
            val lon = minLon + (maxLon - minLon) * random.nextDouble()
            val newX = Math.toRadians(lat)
            val newY = Math.toRadians(lon)
            val time = System.currentTimeMillis()
            val result = kdTree.nearest(KdPoint(newX, newY), 100.0 / Util.EARTH_RADIUS_M)
            val locateTime = System.currentTimeMillis() - time
            if (result.closest != null) {
                successfulCount += 1
                totalTime += locateTime
                totalVisited += result.visitedNodes
            }
            maxTime = max(maxTime, locateTime)
            maxVisited = max(maxVisited, result.visitedNodes)
            minVisited = min(minVisited, result.visitedNodes)
        }
        println("Successful = ${String.format("%.1f", 100 * successfulCount.toDouble() / repeatCount)}%")
        println("Total tree nodes = ${kdTree.size}")
        println("Average locate time = ${String.format("%.1f", totalTime.toDouble() / successfulCount)}ms")
        println("Average visited nodes = ${totalVisited / successfulCount}")
        println("Max time = ${maxTime}ms")
        println("Max visited nodes = $maxVisited")
        println("Min visited nodes = $minVisited")
        assertTrue { maxVisited < 0.01 * kdTree.size }
    }
}