package top.anagke.kwormhole.util

import java.io.File

fun ByteArray.forEachBlock(blockSize: Int, action: (buffer: ByteArray, bytesRead: Int) -> Unit) {
    val arr = ByteArray(blockSize.coerceAtLeast(4.KiB))
    inputStream().use { input ->
        do {
            val size = input.read(arr)
            if (size <= 0) {
                break
            } else {
                action(arr, size)
            }
        } while (true)
    }
}

inline fun <T> File.use(block: (File) -> T): T {
    try {
        return block(this)
    } finally {
        this.deleteRecursively()
    }
}