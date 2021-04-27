package top.anagke.kwormhole.test

import java.io.File

inline fun File.useDir(block: () -> Unit) {
    try {
        this.deleteRecursively()
        this.mkdirs()
        return block()
    } finally {
        this.deleteRecursively()
    }
}