package top.anagke.kwormhole.model

import top.anagke.kio.bytes
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.parseKfrPath
import top.anagke.kwormhole.test.TEST_DIR

fun Model.put(record: Kfr, content: ByteArray?) {
    val disk = record.toFile(TEST_DIR)
    if (content != null) disk.bytes = content

    put(record, disk)
}

fun Model.getContent(path: String): Pair<Kfr?, ByteArray?> {
    val disk = parseKfrPath(TEST_DIR, path)

    val kfr = getContent(path, disk) ?: return (null to null)
    return if (kfr.exists()) {
        (kfr to disk.bytes)
    } else {
        (kfr to null)
    }
}