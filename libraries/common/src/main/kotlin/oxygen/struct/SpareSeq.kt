package oxygen.struct

class SpareSeq<T : Any> : Iterable<T> {
    val elements: ArrayList<T?> = ArrayList()
    val spare = ArrayDeque<Int>()
    var size = 0
        private set

    fun add(value: T): Int {
        size++
        if (spare.isEmpty()) {
            elements.add(value)
            return elements.size - 1
        } else {
            val index = spare.removeFirst()
            elements[index] = value
            return index
        }
    }

    fun remove(index: Int): T? {
        if (index < 0 || index >= elements.size) {
            return null
        }
        val value = elements[index] ?: return null
        size--
        spare.add(index)
        elements[index] = null
        return value
    }

    fun set(index: Int, value: T?): T? {
        if (index < 0 || index >= elements.size) {
            return null
        }
        val ori = elements[index]
        elements[index] = value
        return ori
    }


    fun get(index: Int): T? {
        return elements[index]
    }

    fun clear() {
        size = 0
        elements.clear()
        spare.clear()
    }

    override fun iterator(): Iterator<T> =
        elements.filterNotNull().iterator()
}
