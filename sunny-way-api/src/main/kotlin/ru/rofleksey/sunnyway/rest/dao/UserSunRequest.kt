package ru.rofleksey.sunnyway.rest.dao

import ru.rofleksey.sunnyway.rest.types.GeoPoint

data class UserSunRequest(
    val center: GeoPoint,
    val curTime: Long
)