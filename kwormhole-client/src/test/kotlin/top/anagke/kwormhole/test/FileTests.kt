package top.anagke.kwormhole.test

import java.io.File
import java.io.IOException

inline fun File.useDir(block: () -> Unit) {
    try {
        val successful = this.deleteRecursively()
        if (!successful) throw IOException("deletion of directory $this failed")
        this.mkdirs()
        return block()
    } finally {
        val successful = this.deleteRecursively()
        if (!successful) throw IOException("deletion of directory $this failed")
    }
}