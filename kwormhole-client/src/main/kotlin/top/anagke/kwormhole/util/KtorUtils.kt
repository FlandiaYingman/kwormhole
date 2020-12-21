package top.anagke.kwormhole.util

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.RangeUnits

fun HttpMessageBuilder.contentRange(range: IntRange, total: Long) {
    headers[HttpHeaders.ContentRange] = "${RangeUnits.Bytes} ${range.first}-${range.last}/$total"
}

fun parseRange(rangeString: String): IntRange {
    val (_, rangeTotal) = rangeString.splitPair(" ")
    val (range, _) = rangeTotal.splitPair("/")
    val (rangeStart, rangeEnd) = range.splitPair("-")
    return rangeStart.toInt()..rangeEnd.toInt()
}
