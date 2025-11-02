package oxygen.struct

class SpareSeq<T : Any> : Iterable<T> {
    val elements : ArrayList<T?> = ArrayList()
    val spare = ArrayDeque<Int>()
    public var size = 0
        private set

    fun add(value : T){
        size++
        if(spare.isEmpty()){
            elements.add(value)
        }else{
            elements[spare.removeFirst()] = value
        }
    }

    fun remove(index : Int) : T?{
        if(index < 0 || index >= elements.size){
            return null
        }
        val value = elements[index]
        if(value == null) return null
        spare.add(index)
	val tmp = elements[index]
        elements[index] = null
	return tmp
    }
    
    fun get(index : Int) : T?{
        return elements[index]
    }

    override fun iterator(): Iterator<T> = 
        elements.filterNotNull().iterator()
}
