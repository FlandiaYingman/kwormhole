package top.anagke.kwormhole.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


inline fun <reified T> Gson.fromJson(json: String): T? = fromJson(json, object : TypeToken<T>() {}.type)

inline fun <reified T : List<T>> Gson.listFromJson(json: String): T? = this.fromJson(json)
