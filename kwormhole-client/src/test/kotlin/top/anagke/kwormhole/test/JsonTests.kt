package top.anagke.kwormhole.test

import com.google.gson.Gson
import top.anagke.kwormhole.util.fromJson

val gson = Gson()

inline fun <reified T> toJson(obj: T?): String {
    return gson.toJson(obj)
}

inline fun <reified T> fromJson(json: String): T? {
    return gson.fromJson<T>(json)
}