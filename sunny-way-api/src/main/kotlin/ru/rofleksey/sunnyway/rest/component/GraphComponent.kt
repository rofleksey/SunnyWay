package ru.rofleksey.sunnyway.rest.component

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.rofleksey.sunnyway.graph.Graph
import ru.rofleksey.sunnyway.graph.GraphBuilder
import ru.rofleksey.sunnyway.rest.types.GeoPoint
import ru.rofleksey.sunnyway.util.Util
import ru.rofleksey.sunnyway.util.csv.CsvEdge
import ru.rofleksey.sunnyway.util.csv.CsvReader
import ru.rofleksey.sunnyway.util.kdtree.KdPoint
import ru.rofleksey.sunnyway.util.kdtree.KdTree
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.charset.Charset
import kotlin.system.exitProcess


@Component
class GraphComponent(private val shutdownManager: ShutdownManager) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(GraphComponent::class.java)
    }

    val graph: Graph
    private val kdTree: KdTree

    val vertexList get() = graph.vertexList
    val edgeCount get() = graph.edgeCount

    init {
        log.info("Reading edges...")
        try {
            val file = File("data", "edges.csv")
            if (!file.exists()) {
                throw IOException("can't find $file file")
            }
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
            log.info("Building graph...")
            val builder = GraphBuilder(edges.size)
            edges.forEach { edge ->
                builder.addEdge(edge)
            }
            val (graph, kdTree) = builder.build()
            this.graph = graph
            this.kdTree = kdTree
        } catch (e: Exception) {
            log.error("Error reading graph: {}", e.message)
            shutdownManager.shutdown(1)
            exitProcess(1)
        }
    }

    @Synchronized
    fun locate(point: GeoPoint, maxDistance: Double): Int? {
        val newX = Math.toRadians(point.lat)
        val newY = Math.toRadians(point.lon)
        val newDist = maxDistance / Util.EARTH_RADIUS_M
        return kdTree.nearest(KdPoint(newX, newY), newDist).closest?.id
    }

    @Synchronized
    fun locateAll(point: GeoPoint, distance: Double): List<Int> {
        val newX = Math.toRadians(point.lat)
        val newY = Math.toRadians(point.lon)
        val newDist = distance / Util.EARTH_RADIUS_M
        return kdTree.allNearest(KdPoint(newX, newY), newDist)
    }
}