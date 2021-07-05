package top.anagke.kwormhole.util

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SharedCloseable<T : Closeable>
internal constructor(val closeableObj: T) : Closeable {

    private val user: AtomicInteger = AtomicInteger(1)

    fun increment(): SharedCloseable<T> {
        if (user.getAndIncrement() <= 0) {
            //Before increment the user <= 0, which means the closeable obj has been closed.
            throw IllegalStateException("shared closeable has been closed")
        }
        return this
    }

    fun decrement(): SharedCloseable<T> {
        if (user.decrementAndGet() <= 0) {
            //After decrement the user <= 0, which means no user is using the closeable obj.
            closeableObj.close()
        }
        return this
    }

    override fun close() {
        decrement()
    }

}


private val sharedMap: MutableMap<Any, SharedCloseable<Closeable>> = ConcurrentHashMap()

@Suppress("UNCHECKED_CAST")
fun <K, T : Closeable> shared(key: K, closeable: () -> Closeable): SharedCloseable<T> {
    return sharedMap.compute(key as Any) { _, shared -> shared?.increment() ?: SharedCloseable(closeable()) }
            as SharedCloseable<T>
}