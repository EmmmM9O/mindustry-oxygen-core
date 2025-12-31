package oxygen.annotations

import arc.util.serialization.*

fun jvalFrom(value: Any?): Jval =
    if (value == null) Jval.NULL
    else when (value) {
        is String -> Jval.valueOf(value)
        is Boolean -> Jval.valueOf(value)
        is Int -> Jval.valueOf(value)
        is Float -> Jval.valueOf(value)
        is Double -> Jval.valueOf(value)
        is Array<*> -> Jval.newArray().apply {
            value.forEach { add(jvalFrom(it)) }
        }

        is List<*> -> Jval.newArray().apply {
            value.forEach { add(jvalFrom(it)) }
        }

        else -> Jval.NULL
    }

