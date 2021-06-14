package top.anagke.kwormhole.model

import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import java.io.Closeable
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * A model of KFR.
 */
interface Model : Closeable {

    /**
     * Any changes made to this model.
     *
     * The changes could be made by manually calling [Model.put] or internal update by the implementation.
     *
     * Note that the model should only be used in single thread. Hence, if multiple threads are using this property,
     * every thread might receive only part of changes.
     */
    val changes: BlockingQueue<Kfr>


    /**
     * Gets the KFR corresponded to the given path.
     * @return the KFR, or `null` if absent
     */
    fun head(path: String): Kfr?

    /**
     * Gets the fat KFR corresponded to the given path.
     * @return the fat KFR, or `null` if absent
     */
    fun get(path: String): FatKfr?


    /**
     * Puts a fat KFR to this model.
     */
    fun put(fatKfr: FatKfr)

    /**
     * Check if the KFR can replace the KFR with same path in this model (if present).
     */
    fun acceptable(kfr: Kfr): Boolean {
        val oldKfr = head(kfr.path)
        return kfr.canReplace(oldKfr)
    }


    /**
     * Close this model.
     *
     * Any changes after closing to this model should be ignored.
     */
    override fun close()

}

abstract class AbstractModel : Model {

    private var runner: Thread? = null

    fun open() {
        runner = thread {
            try {
                init()
                while (Thread.interrupted().not()) {
                    poll()
                }
            } catch (ignore: InterruptedException) {
            }
        }
    }


    override val changes: BlockingQueue<Kfr> = LinkedBlockingQueue()


    open fun init() {}

    open fun poll() {}


    override fun close() {
        runner?.interrupt()
        runner?.join()
    }

}

