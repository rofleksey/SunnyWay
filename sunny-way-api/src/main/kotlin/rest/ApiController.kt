package rest

import graph.Graph
import graph.VertexLocator
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import nav.NavigationRequest
import rest.service.NavigationService
import rest.service.ServiceAreaService
import rest.service.ShadowMapService

class ApiController(graph: Graph, private val vertexLocator: VertexLocator) {
    companion object {
        private const val MAX_DISTANCE_DEFAULT = 100.0
    }

    private val navigationService = NavigationService(graph)
    private val serviceAreaService = ServiceAreaService(graph)
    private val shadowMapService = ShadowMapService(graph, vertexLocator)

    fun getServiceArea(ctx: Context) {
        val list = serviceAreaService.getServiceArea()
        ctx.json(UserPolygonResponse(list))
    }

    fun getShadowMap(ctx: Context) {
        val request = ctx.bodyAsClass(UserShadowMapRequest::class.java)
        val map = shadowMapService.getShadowMap(request.center, request.radius)
        ctx.json(UserShadowMapResponse(map))
    }

    fun navigate(ctx: Context) {
        val userRequest = ctx.bodyAsClass(UserNavigationRequest::class.java)
        val locationTime = System.currentTimeMillis()
        val fromId = vertexLocator.locate(userRequest.from, MAX_DISTANCE_DEFAULT)
            ?: throw BadRequestResponse("Failed to locate starting point")
        val toId = vertexLocator.locate(userRequest.to, MAX_DISTANCE_DEFAULT)
            ?: throw BadRequestResponse("Failed to locate ending point")
        println("Located points in ${System.currentTimeMillis() - locationTime} ms")
        val algorithm =
            Algorithm.fromString(userRequest.algorithm) ?: throw BadRequestResponse("Invalid algorithm type")
        val navRequest =
            NavigationRequest(fromId, toId, userRequest.curTime, userRequest.timeSampling, userRequest.preferShadow)
        println("Processing $fromId $toId $algorithm...")
        val processingTime = System.currentTimeMillis()
        val result = navigationService.navigate(navRequest, algorithm)
        println("Done $fromId $toId $algorithm in ${System.currentTimeMillis() - processingTime} ms")
        if (result.path.isEmpty()) {
            throw BadRequestResponse("No path found")
        }
        val response = UserNavigationResponse(result)
        ctx.json(response)
    }
}