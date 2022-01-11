package nav

import graph.Graph
import rest.NavigationEdge

interface Navigator {
    fun navigate(req: NavigationRequest): List<NavigationEdge>
}