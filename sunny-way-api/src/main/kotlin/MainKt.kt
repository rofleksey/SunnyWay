import util.csv.CsvEdge
import util.csv.CsvReader
import rest.GeoPoint
import graph.GraphBuilder
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import rest.ApiController
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.lang.Double.parseDouble
import java.nio.charset.Charset
import kotlin.system.exitProcess

class MainKt {
    companion object {
        private fun initGraph(): GraphBuilder.Result {
            val file = File("data", "edges.csv")
            if (!file.exists()) {
                throw IOException("can't find $file file")
            }
            val edges = CsvReader(FileReader(file, Charset.forName("UTF-8"))).use { reader ->
                reader.readHeader()
                reader.sequence().map { row ->
                    val start = GeoPoint(parseDouble(row["start_lat"]), parseDouble(row["start_lon"]))
                    val end = GeoPoint(parseDouble(row["end_lat"]), parseDouble(row["end_lon"]))
                    CsvEdge(
                        start = start,
                        end = end,
                        leftShadow = parseDouble(row["left_shadow"]),
                        rightShadow = parseDouble(row["right_shadow"]),
                        distance = parseDouble(row["distance"]),
                        direction = parseDouble(row["direction"]),
                    )
                }.toList()
            }
            System.gc()
            val builder = GraphBuilder(edges.size)
            edges.forEach { edge ->
                builder.addEdge(edge)
            }
            return builder.build()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            println("Loading graph...")
            val (graph, locator) = try {
                initGraph()
            } catch (e: Exception) {
                e.printStackTrace()
                System.err.println("Error reading graph: ${e.message}")
                exitProcess(1)
            }

            val controller = ApiController(graph, locator)

            val app = Javalin.create { config ->
                config.enableCorsForAllOrigins()
                config.addStaticFiles("/", "ui/build", Location.EXTERNAL)
                config.addStaticFiles("/data", "data", Location.EXTERNAL)
            }
            app.start(7000)
            app.post("/api/nav", controller::navigate)
            app.post("/api/service-area", controller::getServiceArea)
            app.post("/api/shadow-map", controller::getShadowMap)
        }
    }
}