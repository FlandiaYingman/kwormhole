package top.anagke.kwormhole.controller

import com.fasterxml.jackson.annotation.JsonProperty
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.ThinKfr

data class JsonKfr(
    @JsonProperty("path") val path: String,
    @JsonProperty("time") val time: Long,
    @JsonProperty("size") val size: Long,
    @JsonProperty("hash") val hash: Long,
) {
    fun toKfr(): Kfr = Kfr(path, time, size, hash)
}

data class JsonRange(
    @JsonProperty("begin") val begin: Long,
    @JsonProperty("end") val end: Long
) {
    fun toRange(): ThinKfr.Range = ThinKfr.Range(begin, end)
}