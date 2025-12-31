package oxygen.util

import oxygen.struct.*
import kotlin.reflect.*

typealias OEventListener<T> = (event: T) -> Unit

class EventHandler<T : Any>(
    val manager: EventManager,
    val type: KClass<T>,
    var id: Int,
    var listener: OEventListener<T>
) {
    fun getActive() = manager.get(type, id) == listener

    fun emit(event: T): EventHandler<T> {
        listener(event)
        return this
    }

    fun cancel(): EventHandler<T> {
        manager.cancel(type, id)
        return this
    }

    fun resume(): EventHandler<T> {
        manager.set(type, id, listener)
        return this
    }

    fun remove(): EventHandler<T> {
        manager.remove(type, id)
        return this
    }

    fun reset(listener: OEventListener<T>): EventHandler<T> {
        this.listener = listener
        manager.set(type, id, listener)
        return this
    }

    fun pack(func: EventHandler<T>.(event: T) -> Unit) = reset { func(it) }
}

class EventManager {
    private val listeners = mutableMapOf<KClass<*>, SpareSeq<OEventListener<*>>>()

    fun <T : Any> on(type: KClass<T>, listener: OEventListener<T>): EventHandler<T> =
        EventHandler(this, type, getListNotNull(type).add(listener), listener)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getList(type: KClass<T>): SpareSeq<OEventListener<T>>? =
        listeners[type] as? SpareSeq<OEventListener<T>>

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getListNotNull(type: KClass<T>): SpareSeq<OEventListener<T>> =
        listeners.getOrPut(type) { SpareSeq() } as SpareSeq<OEventListener<T>>

    fun <T : Any> get(type: KClass<T>, id: Int): OEventListener<T>? = getList(type)?.get(id)

    fun <T : Any> cancel(type: KClass<T>, id: Int) {
        getList(type)?.set(id, null)
    }

    fun <T : Any> set(type: KClass<T>, id: Int, listener: OEventListener<T>) {
        getList(type)?.set(id, listener)
    }

    fun <T : Any> remove(type: KClass<T>, id: Int) = getList(type)?.remove(id)

    inline fun <reified T : Any> emit(event: T) {
        getList(T::class)?.forEach { it(event) }
    }

    fun <T : Any> clear(type: KClass<T>) {
        listeners.remove(type)
    }

    fun <T : Any> count(type: KClass<T>): Int = listeners[type]?.size ?: 0

    fun clearAll(): Unit = listeners.clear()
}
