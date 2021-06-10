package top.anagke.kwormhole.model

import top.anagke.kwormhole.Kfr
import java.io.Closeable
import java.io.File
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
     * Puts a record and the corresponding content to this model.
     * @param record the record to be put
     * @param content the content to be put
     */
    fun put(record: Kfr, content: File?)

    fun getRecord(path: String): Kfr?

    fun getContent(path: String, file: File): Kfr?


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


    abstract fun init()

    abstract fun poll()

    override fun close() {
        runner?.interrupt()
        runner?.join()
    }


    fun Kfr.isValid(): Boolean {
        return this.canReplace(getRecord(path))
    }

}

