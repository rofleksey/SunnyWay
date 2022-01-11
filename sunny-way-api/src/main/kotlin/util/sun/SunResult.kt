package util.sun

data class SunResult(
    val declination: Double,
    val eot: Double,
    val lstm: Double,
    val timeCorrection: Double,
    val localSolarTime: Double,
    val elevation: Double,
    val zenith: Double,
    val azimuth: Double,
    val sunrise: Double,
    val sunset: Double
)