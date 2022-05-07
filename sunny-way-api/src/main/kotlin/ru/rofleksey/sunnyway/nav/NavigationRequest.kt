package ru.rofleksey.sunnyway.nav

data class NavigationRequest (
    val fromId: Int,
    val toId: Int,
    val curTime: Long,
    val maxFactor: Double,
    val preferShadow: Boolean,
)