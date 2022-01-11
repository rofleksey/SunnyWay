package nav

data class NavigationRequest (
    val fromId: Int,
    val toId: Int,
    val curTime: Long,
    val timeSampling: Long,
    val preferShadow: Boolean,
)