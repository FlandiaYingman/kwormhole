package top.anagke.kwormhole.model

import top.anagke.kio.bytes
import top.anagke.kwormhole.KFR
import top.anagke.kwormhole.test.TEST_DIR
import top.anagke.kwormhole.toDiskPath

fun Model.put(record: KFR, content: ByteArray?) {
    val disk = record.path.toDiskPath(TEST_DIR)
    if (content != null) disk.bytes = content

    put(record, disk)
}

fun Model.getContent(path: String): Pair<KFR?, ByteArray?> {
    val disk = path.toDiskPath(TEST_DIR)
    val kfr = getContent(path, disk) ?: return (null to null)
    return if (kfr.representsExisting()) {
        (kfr to disk.bytes)
    } else {
        (kfr to null)
    }
}