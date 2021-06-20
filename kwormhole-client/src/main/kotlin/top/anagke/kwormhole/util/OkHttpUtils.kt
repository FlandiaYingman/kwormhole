package top.anagke.kwormhole.util

data class FormDisposition(
    val name: String,
    val filename: String?
)

fun parseFormDisposition(string: String): FormDisposition {
    val map = string.split(";")
        .map { it.trim() }
        .map { it.split('=') }
        .onEach { if (it.size == 1) check(it[0] == "form-data") }
        .filter { it.size != 1 }
        .associate { it[0] to it[1].trim('"') }
    val name = map["name"]
    val filename = map["filename"]
    return FormDisposition(name!!, filename)
}