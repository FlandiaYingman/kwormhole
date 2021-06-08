package top.anagke.kwormhole.model

import top.anagke.kio.bytes
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.test.TEST_DIR
import top.anagke.kwormhole.resolveBy

fun Model.put(record: Kfr, content: ByteArray?) {
    val disk = record.path.resolveBy(TEST_DIR)
    if (content != null) disk.bytes = content

    put(record, disk)
}

fun Model.getContent(path: String): Pair<Kfr?, ByteArray?> {
    val disk = path.resolveBy(TEST_DIR)
    val kfr = getContent(path, disk) ?: return (null to null)
    return if (kfr.representsExisting()) {
        (kfr to disk.bytes)
    } else {
        (kfr to null)
    }
}