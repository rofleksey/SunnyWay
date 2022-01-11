package rest

data class UserNavigationRequest(
    val from: GeoPoint,
    val to: GeoPoint,
    val algorithm: String,
    val curTime: Long,
    val timeSampling: Long,
    val preferShadow: Boolean,
)