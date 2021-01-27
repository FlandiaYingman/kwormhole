package top.anagke.kwormhole.util


val Int.B: Int get() = this
val Int.KiB: Int get() = this * 2.shl(10 - 1)
val Int.MiB: Int get() = this * 2.shl(20 - 1)
val Int.GiB: Int get() = Math.toIntExact(this.toLong() * 2.toLong().shl(30 - 1))

val Long.KiB: Long get() = this * 2L.shl(10 - 1)
val Long.MiB: Long get() = this * 2L.shl(20 - 1)
val Long.GiB: Long get() = this * 2L.shl(30 - 1)
val Long.TiB: Long get() = this * 2L.shl(40 - 1)
