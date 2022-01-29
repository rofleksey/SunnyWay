package ru.rofleksey.sunnyway.nav

import ru.rofleksey.sunnyway.rest.types.NavigationEdge

interface Navigator {
    fun navigate(req: NavigationRequest): List<NavigationEdge>
}