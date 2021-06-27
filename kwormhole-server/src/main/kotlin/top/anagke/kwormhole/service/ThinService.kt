package top.anagke.kwormhole.service

import org.springframework.stereotype.Service
import top.anagke.kio.util.TempFiles
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.ThinKfr
import top.anagke.kwormhole.merge
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@Service
class ThinService {

    private val mapTempFile: MutableMap<Kfr, Path> = ConcurrentHashMap()

    fun mergeAlloc(thin: ThinKfr): FatKfr? {
        val temp = mapTempFile.getOrPut(Kfr(thin)) { TempFiles.allocLocal() }
        val fat = thin.merge(temp)
        return fat
    }

    fun mergeFree(kfr: Kfr) {
        mapTempFile.remove(Kfr(kfr))
    }

}