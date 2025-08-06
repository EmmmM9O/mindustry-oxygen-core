package oxygen.ksp

import arc.util.serialization.*
import oxygen.annotations.*

object KspUtil {
    val json: Json = object : Json() {
        override fun writeValue(value: Any?, knownType: Class<*>?, elementType: Class<*>?) {
            if (value is BaseLocationInfo) {
                writeLocation(this, value)
            } else
                super.writeValue(value, knownType, elementType)
        }
    }
}