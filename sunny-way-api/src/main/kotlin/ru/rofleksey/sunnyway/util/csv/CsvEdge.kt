package ru.rofleksey.sunnyway.util.csv

import ru.rofleksey.sunnyway.rest.types.GeoPoint

data class CsvEdge(
    val start: GeoPoint,
    val end: GeoPoint,
    val leftShadow: Double,
    val rightShadow: Double,
    val distance: Double,
    val direction: Double,
)