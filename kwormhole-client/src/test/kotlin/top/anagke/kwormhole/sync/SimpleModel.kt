//package top.anagke.kwormhole.sync
//
//import top.anagke.kwormhole.FileContent
//import top.anagke.kwormhole.KFR
//import top.anagke.kwormhole.model.FileContentProvider
//import top.anagke.kwormhole.model.Model
//import java.util.concurrent.BlockingQueue
//import java.util.concurrent.ConcurrentHashMap
//import java.util.concurrent.LinkedBlockingQueue
//
//class SimpleModel(
//    val name: String,
//    rcs: List<Pair<KFR, FileContent>> = emptyList(),
//) : Model {
//
//    override val records: Map<String, KFR> = ConcurrentHashMap()
//    override val contents: Map<String, FileContentProvider> = ConcurrentHashMap()
//    override val changes: BlockingQueue<String> = LinkedBlockingQueue()
//
//    init {
//        rcs.forEach { (record, content) ->
//            put(record, content)
//        }
//    }
//
//    override fun put(record: KFR, content: FileContent) {
//        records as MutableMap
//        contents as MutableMap
//        records[record.path] = record
//        contents[record.path] = { content }
//        changes.offer(record.path)
//    }
//
//    override fun close() {
//    }
//
//    override fun toString(): String = "SimpleModel($name)"
//
//}