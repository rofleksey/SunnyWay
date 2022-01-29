package ru.rofleksey.sunnyway.rest.dao

import ru.rofleksey.sunnyway.rest.types.EdgeWithCost

data class UserShadowMapResponse(val edges: List<EdgeWithCost>)