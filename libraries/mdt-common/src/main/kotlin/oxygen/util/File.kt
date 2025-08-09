package oxygen.util

import arc.files.*

fun Fi.childPath(path: String): Fi {
    var file = this
    path.split("/").forEach {
        file = file.child(it)
    }
    return file
}