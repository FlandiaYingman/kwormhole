package top.anagke.kwormhole.model

import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.shouldReplace
import java.io.Closeable
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

typealias FileContentProvider = () -> FileContent

/**
 * A model of KFR.
 */
interface Model : Closeable {

    /**
     * The records in this model.
     */
    val records: Map<String, FileRecord>

    /**
     * The contents in this model.
     */
    val contents: Map<String, FileContentProvider>

    /**
     * Any changes made to this model.
     *
     * The changes could be made by manually calling [Model.put] or internal update by the implementation.
     *
     * Note that the model should only be used in single thread. Hence, if multiple threads are using this property,
     * every thread might receive only part of changes.
     */
    val changes: BlockingQueue<String>


    /**
     * Puts a record and the corresponding content to this model.
     * @param record the record to be put
     * @param content the content to be put
     */
    fun put(record: FileRecord, content: FileContent)

    /**
     * Close this model.
     *
     * Any changes after closing to this model should be ignored.
     */
    override fun close()

}

abstract class AbstractModel : Model {

    private var runner: Thread? = null

    fun start() {
        runner = thread {
            val init = init()
            init.forEach { (record, content) -> submit(record, content) }
            while (Thread.interrupted().not()) {
                val pollList = poll()
                pollList.forEach { (record, content) -> submit(record, content) }
            }
        }
    }


    override val records: Map<String, FileRecord> by lazy { mutableRecords }
    private val mutableRecords: MutableMap<String, FileRecord> = ConcurrentHashMap()

    override val contents: Map<String, FileContentProvider> by lazy { mutableContents }
    private val mutableContents: MutableMap<String, FileContentProvider> = ConcurrentHashMap()

    override val changes: BlockingQueue<String> = LinkedBlockingQueue()


    override fun put(record: FileRecord, content: FileContent) {
        if (record.submittable()) {
            onPut(record, content)
            submit(record, content)
        }
    }

    abstract fun onPut(record: FileRecord, content: FileContent)


    abstract fun init(): List<Pair<FileRecord, FileContent>>

    abstract fun poll(): List<Pair<FileRecord, FileContent>>

    override fun close() {
        runner?.interrupt()
        runner?.join()
    }


    protected fun submit(record: FileRecord, content: FileContent) {
        if (record.submittable()) {
            val path = record.path
            mutableRecords[path] = record
            mutableContents[path] = { content }
            changes.put(path)
        }
    }

    protected fun FileRecord.submittable(): Boolean {
        val existingRecord = mutableRecords[path]
        return this.shouldReplace(existingRecord)
    }

}

