package top.anagke.kwormhole.service

import org.springframework.stereotype.Service
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.ThinKfr
import top.anagke.kwormhole.merge
import top.anagke.kwormhole.util.TempFiles
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@Service
class ThinKfrService {

    private val map: MutableMap<Kfr, Path> = ConcurrentHashMap()

    fun merge(thin: ThinKfr): FatKfr? {
        val temp = map.getOrPut(thin.kfr) { TempFiles.allocTempFile() }
        val fat = thin.merge(temp) { TempFiles.freeTempFile(temp) }
        if (fat != null) {
            map.remove(thin.kfr)
            return fat
        } else {
            return null
        }
    }

}