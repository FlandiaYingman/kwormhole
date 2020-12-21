@file:Suppress("BlockingMethodInNonBlockingContext")

package top.anagke.kwormhole.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal const val MINIMUM_BLOCK_SIZE: Int = 512

suspend fun File.suspendReadBytes(): ByteArray {
    return withContext(Dispatchers.IO) { readBytes() }
}

suspend fun File.suspendForEachBlock(blockSize: Int, action: suspend (buffer: ByteArray, bytesRead: Int) -> Unit) {
    inputStream().use { input ->
        withContext(Dispatchers.IO) {
            val arr = ByteArray(blockSize.coerceAtLeast(MINIMUM_BLOCK_SIZE))
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
}