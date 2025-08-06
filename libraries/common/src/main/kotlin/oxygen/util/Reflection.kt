package oxygen.util

inline fun <reified T> anyToEnumOr(value: String): T? {
    val javaClass = T::class.java
    return if (javaClass.isEnum) {
        javaClass.enumConstants.firstOrNull { it.toString() == value }
    } else {
        null
    }
}

inline fun <reified T : Enum<T>> toEnumOr(name: String): T? = try {
    enumValueOf<T>(name)
} catch (e: IllegalArgumentException) {
    null
}