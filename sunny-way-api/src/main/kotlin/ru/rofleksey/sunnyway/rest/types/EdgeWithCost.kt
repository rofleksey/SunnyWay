package ru.rofleksey.sunnyway.rest.types

data class EdgeWithCost(val fromPoint: GeoPoint, val toPoint: GeoPoint, val factor: Double)