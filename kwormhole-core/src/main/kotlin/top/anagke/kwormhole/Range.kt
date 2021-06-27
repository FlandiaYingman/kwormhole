package top.anagke.kwormhole

data class Range(
    val begin: Long,
    val end: Long
) {

    init {
        require(0 <= begin) { "0 <= begin failed, $this" }
        require(begin <= end) { "begin <= end failed, $this" }
    }

    fun length(): Long = end - begin

    override fun toString(): String {
        return "[$begin, $end)"
    }

}

data class Fraction(
    val numerator: Long,
    val denominator: Long,
) {

    init {
        require(denominator != 0L) { "denominator != 0 failed, $this" }
    }

    override fun toString(): String {
        return "$numerator/$denominator"
    }

}