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
        private val log: Logger = LoggerFactory.getLogger(ApiController::class.java)
    }

    @GetMapping("/service-area")
    fun getServiceArea(): UserPolygonResponse {
        val list = serviceAreaService.getServiceArea()
        return UserPolygonResponse(list)
    }

    @PostMapping("/shadow-map")
    fun getShadowMap(@RequestBody req: UserShadowMapRequest): UserShadowMapResponse {
        val map = shadowMapService.getShadowMap(req.center, req.radius)
        return UserShadowMapResponse(map)
    }

    @PostMapping("/nav")
    fun navigate(@RequestBody req: UserNavigationRequest): UserNavigationResponse {
        val locationTime = System.currentTimeMillis()
        val fromId = graph.locate(req.from, MAX_DISTANCE_DEFAULT)
            ?: throw NavigationException("Failed to locate starting point")
        val toId = graph.locate(req.to, MAX_DISTANCE_DEFAULT)
            ?: throw NavigationException("Failed to locate ending point")
        log.info("Located points in {} ms", System.currentTimeMillis() - locationTime)
        val navRequest = NavigationRequest(fromId, toId, req.curTime, req.timeSampling, req.preferShadow)
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
        return UserNavigationResponse(result)
    }
}