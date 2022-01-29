package ru.rofleksey.sunnyway.rest.dao

import ru.rofleksey.sunnyway.rest.types.Algorithm
import ru.rofleksey.sunnyway.rest.types.GeoPoint

data class UserNavigationRequest(
    val from: GeoPoint,
    val to: GeoPoint,
    val algorithm: Algorithm,
    val curTime: Long,
    val timeSampling: Long,
    val preferShadow: Boolean,
)