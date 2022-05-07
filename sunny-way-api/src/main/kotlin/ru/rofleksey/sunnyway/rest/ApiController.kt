package ru.rofleksey.sunnyway.rest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import ru.rofleksey.sunnyway.nav.NavigationException
import ru.rofleksey.sunnyway.nav.NavigationRequest
import ru.rofleksey.sunnyway.rest.component.GraphComponent
import ru.rofleksey.sunnyway.rest.dao.*
import ru.rofleksey.sunnyway.rest.service.NavigationService
import ru.rofleksey.sunnyway.rest.service.ServiceAreaService
import ru.rofleksey.sunnyway.rest.service.ShadowMapService
import ru.rofleksey.sunnyway.rest.types.NavigationEdge
import ru.rofleksey.sunnyway.util.Util
import ru.rofleksey.sunnyway.util.sun.Sun
import ru.rofleksey.sunnyway.util.sun.SunResult
import java.util.*

@RestController
@RequestMapping("api")
open class ApiController(
    private val navigationService: NavigationService,
    private val serviceAreaService: ServiceAreaService,
    private val shadowMapService: ShadowMapService,
    private val graph: GraphComponent
) : WebMvcConfigurer {
    companion object {
        private const val MAX_DISTANCE_DEFAULT = 100.0
        private const val MAX_SHADOW_MAP_RADIUS = 750.0
        private val log: Logger = LoggerFactory.getLogger(ApiController::class.java)
    }

    @GetMapping("/service-area")
    fun getServiceArea(): UserPolygonResponse {
        val list = serviceAreaService.getServiceArea()
        return UserPolygonResponse(list)
    }

    @PostMapping("/sun")
    fun getSun(@RequestBody req: UserSunRequest): UserSunResponse {
        val result = Sun(req.center.lat, req.center.lon, 1).run {
            setTime(Calendar.getInstance().apply {
                timeInMillis = req.curTime
            })
            calculate()
        }
        return UserSunResponse(result.elevation, result.azimuth)
    }

    @PostMapping("/shadow-map")
    fun getShadowMap(@RequestBody req: UserShadowMapRequest): UserShadowMapResponse {
        if (req.radius >= MAX_SHADOW_MAP_RADIUS) {
            throw NavigationException("Radius too large")
        }
        val map = shadowMapService.getShadowMap(req.center, req.radius, req.time)
        return UserShadowMapResponse(map)
    }

    private fun mapToUser(edges: List<NavigationEdge>, departureTime: Long): List<UserNavigationEdge> {
        if (edges.isEmpty()) {
            return listOf()
        }
        val sun = Sun(edges[0].fromPoint.lat, edges[0].fromPoint.lon, 1)
        var curTime = departureTime
        val calendar = Calendar.getInstance()
        return edges.map { edge ->
            sun.setTime(calendar.apply {
                timeInMillis = curTime
            })
            val traverseTime = (1000.0 * edge.distance / Util.PEDESTRIAN_SPEED).toLong()
            curTime += traverseTime
            val sunResult = sun.calculate()
            val factor = if (sunResult.isSunUp()) {
                sunResult.calculateFactor(
                    edge.direction, edge.leftShadow, edge.rightShadow,
                    true, SunResult.DEFAULT_MAX_FACTOR
                )
            } else {
                0.0
            }
            UserNavigationEdge(
                edge.fromPoint,
                edge.toPoint,
                edge.edgeId,
                edge.toVertexId,
                edge.distance,
                traverseTime,
                factor
            )
        }
    }

    @PostMapping("/nav")
    fun navigate(@RequestBody req: UserNavigationRequest): UserNavigationResponse {
        val locationTime = System.currentTimeMillis()
        val fromId = graph.locate(req.from, MAX_DISTANCE_DEFAULT)
            ?: throw NavigationException("Failed to locate starting point")
        val toId = graph.locate(req.to, MAX_DISTANCE_DEFAULT)
            ?: throw NavigationException("Failed to locate ending point")
        log.info("Located points in {} ms", System.currentTimeMillis() - locationTime)
        val navRequest = NavigationRequest(fromId, toId, req.curTime, req.maxFactor, req.preferShadow)
        log.info("Processing #{}->#{} ({})...", fromId, toId, req.algorithm)
        val processingTime = System.currentTimeMillis()
        val result = navigationService.navigate(navRequest, req.algorithm)
        log.info(
            "Done #{}->#{} ({}) in {} ms",
            fromId,
            toId,
            req.algorithm,
            System.currentTimeMillis() - processingTime
        )
        if (result.path.isEmpty()) {
            log.info("No path found #{}->#{}", fromId, toId)
            throw NavigationException("No path found")
        }
        val userEdges = mapToUser(result.path, req.curTime)
        return UserNavigationResponse(UserNavigationResult(userEdges, result.computeTime))
    }
}