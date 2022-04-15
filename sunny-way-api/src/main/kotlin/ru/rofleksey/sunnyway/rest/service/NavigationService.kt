package ru.rofleksey.sunnyway.rest.service

import org.springframework.stereotype.Service
import ru.rofleksey.sunnyway.bound.RectMapBoundFactory
import ru.rofleksey.sunnyway.nav.DiscreteShadowNavigator
import ru.rofleksey.sunnyway.nav.DistanceNavigator
import ru.rofleksey.sunnyway.nav.NavigationRequest
import ru.rofleksey.sunnyway.rest.component.GraphComponent
import ru.rofleksey.sunnyway.rest.types.Algorithm
import ru.rofleksey.sunnyway.rest.types.NavigationResult

@Service
class NavigationService(graph: GraphComponent) {
    private val mapBoundFactory = RectMapBoundFactory(0.05, 0.1) // TODO: 3 * 1.11 km?
    private val distanceNavigator = DistanceNavigator(graph.graph, mapBoundFactory)
    private val discreteShadowNavigator = DiscreteShadowNavigator(graph.graph, mapBoundFactory)

    fun navigate(req: NavigationRequest, algorithm: Algorithm): NavigationResult {
        val navigator = when (algorithm) {
            Algorithm.DISTANCE -> distanceNavigator
            Algorithm.SHADOW -> discreteShadowNavigator
            else -> throw IllegalStateException("Not implemented")
        }
        val startTime = System.currentTimeMillis()
        val path = navigator.navigate(req)
        val computeTime = System.currentTimeMillis() - startTime
        return NavigationResult(path, computeTime)
    }
}