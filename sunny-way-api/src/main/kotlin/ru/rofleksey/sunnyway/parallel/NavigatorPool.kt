package ru.rofleksey.sunnyway.parallel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.rofleksey.sunnyway.nav.NavigationException
import ru.rofleksey.sunnyway.nav.NavigationRequest
import ru.rofleksey.sunnyway.nav.Navigator
import java.util.concurrent.Executors

abstract class NavigatorPool(shardCount: Int, queueSize: Int) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(NavigatorPool::class.java)
    }

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
                try {
                    actor.loop()
                } catch (e: Throwable) {
                    log.error("fatal navigator actor error", e)
                }
            }
        }
    }

    suspend fun enqueueAndJoin(request: NavigationRequest): NavigatorActorResult {
        val outputChannel = Channel<NavigatorActorResult>(1)
        val actorMessage = NavigatorActorMessage(request, outputChannel)
        val sendResult = jobChannel.trySend(actorMessage)
        if (!sendResult.isSuccess) {
            return NavigatorActorResult.createFailure(NavigationException("Queue is full, try again later"))
        }
        return outputChannel.receive()
    }
}