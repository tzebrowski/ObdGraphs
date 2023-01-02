package org.obd.graphs


class Cache {
    private var cache: MutableMap<String, Any> = mutableMapOf()

    fun findEntry(name: String): Any? = cache[name]

    fun updateEntry(name: String, value: Any) {
        cache[name] = value
    }

    fun initCache(m: MutableMap<String, Any>) {
        cache = m
    }
}

val cacheManager = Cache()
