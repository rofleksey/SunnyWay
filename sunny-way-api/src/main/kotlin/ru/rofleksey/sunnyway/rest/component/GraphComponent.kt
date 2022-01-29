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
import java.lang.Double
import java.nio.charset.Charset
import kotlin.Exception
import kotlin.Int
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
                    val start = GeoPoint(Double.parseDouble(row["start_lat"]), Double.parseDouble(row["start_lon"]))
                    val end = GeoPoint(Double.parseDouble(row["end_lat"]), Double.parseDouble(row["end_lon"]))
                    CsvEdge(
                        start = start,
                        end = end,
                        leftShadow = Double.parseDouble(row["left_shadow"]),
                        rightShadow = Double.parseDouble(row["right_shadow"]),
                        distance = Double.parseDouble(row["distance"]),
                        direction = Double.parseDouble(row["direction"]),
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
    fun locate(point: GeoPoint, maxDistance: kotlin.Double): Int? {
        val newX = Math.toRadians(point.lat)
        val newY = Math.toRadians(point.lon)
        val newDist = maxDistance / Util.EARTH_RADIUS_M
        return kdTree.nearest(KdPoint(newX, newY), newDist)
    }

    @Synchronized
    fun locateAll(point: GeoPoint, distance: kotlin.Double): List<Int> {
        val newX = Math.toRadians(point.lat)
        val newY = Math.toRadians(point.lon)
        val newDist = distance / Util.EARTH_RADIUS_M
        return kdTree.nearestAll(KdPoint(newX, newY), newDist)
    }
}