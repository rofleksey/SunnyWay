package rest

enum class Algorithm {
    DISTANCE, SHADOW;

    companion object {
        fun fromString(s: String): Algorithm? {
            return when (s.toLowerCase()) {
                "distance" -> DISTANCE
                "shadow" -> SHADOW
                else -> null
            }
        }
    }
}