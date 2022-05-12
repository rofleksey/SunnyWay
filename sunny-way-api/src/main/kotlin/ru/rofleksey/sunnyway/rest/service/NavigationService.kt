package ru.rofleksey.sunnyway.rest.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.rofleksey.sunnyway.bound.RectMapBoundFactory
import ru.rofleksey.sunnyway.nav.DiscreteShadowNavigator
import ru.rofleksey.sunnyway.nav.DistanceNavigator
import ru.rofleksey.sunnyway.nav.NavigationRequest
import ru.rofleksey.sunnyway.parallel.NavigatorPool
import ru.rofleksey.sunnyway.rest.component.GraphComponent
import ru.rofleksey.sunnyway.rest.types.Algorithm
import ru.rofleksey.sunnyway.rest.types.NavigationResult
import ru.rofleksey.sunnyway.util.Config

@Service
class NavigationService(graph: GraphComponent, config: Config) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(NavigationService::class.java)
    }

    private val mapBoundFactory = RectMapBoundFactory(0.05, 0.1)

    private val distanceNavigatorPool = object : NavigatorPool(config.shardCount, config.queueSize) {
        override fun createNavigator() = DistanceNavigator(graph.graph, mapBoundFactory)
    }.apply {
        start()
    }

    init {
        log.info("Creating NavigationService with ${config.shardCount} shards and queue size of ${config.queueSize}")
    }

    private val discreteShadowNavigatorPool = object : NavigatorPool(config.shardCount, config.queueSize) {
        override fun createNavigator() = DiscreteShadowNavigator(graph.graph, mapBoundFactory)
    }.apply {
        start()
    }

    suspend fun navigate(req: NavigationRequest, algorithm: Algorithm): NavigationResult {
        val navigatorPool = when (algorithm) {
            Algorithm.DISTANCE -> distanceNavigatorPool
            Algorithm.SHADOW -> discreteShadowNavigatorPool
        }
        val startTime = System.currentTimeMillis()
        val result = navigatorPool.enqueueAndJoin(req)
        if (result.isFailure) {
            throw result.getErr()
        }
        val computeTime = System.currentTimeMillis() - startTime
        return NavigationResult(result.getPath(), computeTime)
    }
}