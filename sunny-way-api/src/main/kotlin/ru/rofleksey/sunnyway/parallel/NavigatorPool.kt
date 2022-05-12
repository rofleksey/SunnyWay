package ru.rofleksey.sunnyway.parallel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import ru.rofleksey.sunnyway.nav.NavigationRequest
import ru.rofleksey.sunnyway.nav.Navigator
import java.util.concurrent.Executors

abstract class NavigatorPool(shardCount: Int, queueSize: Int) {
    private val jobChannel = Channel<NavigatorActorMessage>(queueSize)
    private val actorPool = Array(shardCount) {
        NavigatorActor(createNavigator(), jobChannel)
    }
    private val dispatcher = Executors.newFixedThreadPool(shardCount).asCoroutineDispatcher()
    private val job = Job()
    private val coroutineScope = CoroutineScope(dispatcher + job)

    abstract fun createNavigator(): Navigator

    fun start() {
        actorPool.forEach { actor ->
            coroutineScope.launch {
                actor.loop()
            }
        }
    }

    suspend fun enqueueAndJoin(request: NavigationRequest): NavigatorActorResult {
        val outputChannel = Channel<NavigatorActorResult>(1)
        val actorMessage = NavigatorActorMessage(request, outputChannel)
        val sendResult = jobChannel.trySend(actorMessage)
        if (!sendResult.isSuccess) {
            return NavigatorActorResult.createFailure(RuntimeException("Queue is full, try again later"))
        }
        return outputChannel.receive()
    }
}