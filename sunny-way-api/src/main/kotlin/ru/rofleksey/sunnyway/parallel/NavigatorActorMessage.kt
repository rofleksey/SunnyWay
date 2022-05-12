package ru.rofleksey.sunnyway.parallel

import kotlinx.coroutines.channels.Channel
import ru.rofleksey.sunnyway.nav.NavigationRequest

data class NavigatorActorMessage(val request: NavigationRequest, val outputChannel: Channel<NavigatorActorResult>)