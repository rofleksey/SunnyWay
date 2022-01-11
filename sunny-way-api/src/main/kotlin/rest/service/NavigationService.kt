package rest.service

import bound.RectMapBoundFactory
import graph.Graph
import nav.DiscreteShadowNavigator
import nav.DistanceNavigator
import nav.NavigationRequest
import rest.Algorithm
import rest.NavigationResult
import java.lang.IllegalStateException

class NavigationService(graph: Graph) {
    private val mapBoundFactory = RectMapBoundFactory(0.05, 0.1) // TODO: 3 * 1.11 km?
    private val distanceNavigator = DistanceNavigator(graph, mapBoundFactory)
    private val discreteShadowNavigator = DiscreteShadowNavigator(graph, mapBoundFactory)

    @Synchronized
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