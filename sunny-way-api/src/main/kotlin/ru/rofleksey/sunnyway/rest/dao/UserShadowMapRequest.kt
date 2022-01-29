package ru.rofleksey.sunnyway.rest.dao

import ru.rofleksey.sunnyway.rest.types.GeoPoint

data class UserShadowMapRequest(val center: GeoPoint, val radius: Double)