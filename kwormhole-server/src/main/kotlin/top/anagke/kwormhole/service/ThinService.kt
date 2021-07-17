package top.anagke.kwormhole.service

import org.springframework.stereotype.Service
import top.anagke.kio.util.TempFiles
import top.anagke.kwormhole.*
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@Service
class ThinService {

    private val mapTempFile: MutableMap<Kfr, Path> = ConcurrentHashMap()

    fun mergeAlloc(thin: ThinKfr): FatKfr? {
        val temp = mapTempFile.getOrPut(thin.asPojo()) { TempFiles.allocLocal() }
        val fat = thin.merge(temp) { TempFiles.free(it) }
        return fat
    }

    fun mergeFree(fat: FatKfr) {
        val kfr = fat.asPojo()
        check(kfr in mapTempFile)

        mapTempFile.remove(kfr)
        fat.close()
    }

}