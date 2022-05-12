package ru.rofleksey.sunnyway.parallel

import ru.rofleksey.sunnyway.rest.types.NavigationEdge

class NavigatorActorResult private constructor(
    private val path: List<NavigationEdge>?,
    private val err: Throwable?
) {
    companion object {
        fun createSuccess(path: List<NavigationEdge>) = NavigatorActorResult(path, null)
        fun createFailure(err: Throwable) = NavigatorActorResult(null, err)
    }

    val isSuccess: Boolean get() = path != null
    val isFailure: Boolean get() = err != null

    fun getPath() = path!!
    fun getErr() = err!!
}