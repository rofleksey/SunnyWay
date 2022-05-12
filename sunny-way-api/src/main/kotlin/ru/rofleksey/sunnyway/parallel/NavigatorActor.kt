package ru.rofleksey.sunnyway.parallel

import kotlinx.coroutines.channels.Channel
import ru.rofleksey.sunnyway.nav.Navigator

class NavigatorActor(private val navigator: Navigator, private val jobChannel: Channel<NavigatorActorMessage>) {
    suspend fun loop() {
        while (true) {
            val (request, outputChannel) = jobChannel.receive()
            try {
                val result = navigator.navigate(request)
                outputChannel.send(NavigatorActorResult.createSuccess(result))
            } catch (e: Throwable) {
                outputChannel.send(NavigatorActorResult.createFailure(e))
            }
        }
    }
}