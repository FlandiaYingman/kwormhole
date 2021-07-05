package top.anagke.kwormhole

import top.anagke.kwormhole.sync.utcEpochMillis
import top.anagke.kwormhole.util.Hasher
import java.io.File
import java.util.*
import kotlin.random.Random

object MockKfr {

    fun mockString(): String {
        return UUID.randomUUID().toString()
    }

    fun mockPath(maxDepth: Int = 4): String {
        return List(Random.nextInt(1, maxDepth + 1)) { "/${mockString()}" }.joinToString("")
    }

    fun mockFile(parent: File, maxDepth: Int = 4): File {
        return parsePath(parent, mockPath(maxDepth))
    }


    fun mockKfr(maxSize: Int = 4096): Kfr {
        val path = mockPath()
        val time = utcEpochMillis
        val size = Random.nextLong(maxSize.toLong())
        val hash = Random.nextLong()
        return Kfr(path, time, size, hash)
    }

    fun mockFatKfr(maxSize: Int = 4096, maxDepth: Int = 4): FatKfr {
        val content = Random.nextBytes(Random.nextInt(maxSize))

        val path = mockPath(maxDepth)
        val time = utcEpochMillis
        val size = content.size.toLong()
        val hash = Hasher.hash(content)

        return tempFatKfr(Kfr(path, time, size, hash), content)
    }

    fun mockOnFile(root: File, maxSize: Int = 4096, maxDepth: Int = 4): Pair<FatKfr, File> {
        val kfrContent = mockFatKfr(maxSize, maxDepth)
        val file = parsePath(root, kfrContent.path)
        kfrContent.copy(file.toPath())
        return (kfrContent to file)
    }

}