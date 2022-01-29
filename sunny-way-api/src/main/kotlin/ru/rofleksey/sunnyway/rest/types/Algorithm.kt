package ru.rofleksey.sunnyway.rest.types

import com.fasterxml.jackson.annotation.JsonCreator

enum class Algorithm(private val text: String) {
    DISTANCE("distance"), SHADOW("shadow");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromText(text: String): Algorithm {
            return when (text.toLowerCase()) {
                "distance" -> DISTANCE
                "shadow" -> SHADOW
                else -> throw IllegalArgumentException()
            }
        }
    }

    override fun toString(): String = text
}