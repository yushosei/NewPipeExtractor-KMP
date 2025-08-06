package com.yushosei.newpipe.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import com.yushosei.newpipe.extractor.Info
import com.yushosei.newpipe.extractor.InfoItem

const val NO_SERVICE_ID = -1

class SimpleLruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()
    private val accessOrder = mutableListOf<K>()

    fun put(key: K, value: V) {
        if (map.containsKey(key)) {
            accessOrder.remove(key)
        }
        map[key] = value
        accessOrder.add(key)

        if (map.size > maxSize) {
            val eldestKey = accessOrder.removeAt(0)
            map.remove(eldestKey)
        }
    }

    fun get(key: K): V? {
        val value = map[key] ?: return null
        accessOrder.remove(key)
        accessOrder.add(key)
        return value
    }

    fun remove(key: K) {
        map.remove(key)
        accessOrder.remove(key)
    }

    fun clear() {
        map.clear()
        accessOrder.clear()
    }

    fun size(): Int = map.size

    fun trimToSize(targetSize: Int) {
        while (map.size > targetSize) {
            val eldestKey = accessOrder.removeAt(0)
            map.remove(eldestKey)
        }
    }

    fun snapshot(): Map<K, V> = map.toMap()
}

class InfoCache private constructor() {

    private val mutex = Mutex()
    private val cache = SimpleLruCache<String, CacheData>(MAX_ITEMS_ON_CACHE)

    suspend fun getFromKey(
        serviceId: Int,
        url: String,
        infoType: InfoItem.InfoType
    ): Info? = mutex.withLock {
        getInfo(keyOf(serviceId, url, infoType))
    }

    suspend fun putInfo(
        serviceId: Int,
        url: String,
        info: Info,
        infoType: InfoItem.InfoType
    ) = mutex.withLock {
        val expirationMillis: Long = ServiceHelper.getCacheExpirationMillis(info.serviceId)
        cache.put(keyOf(serviceId, url, infoType), CacheData(info, expirationMillis))
    }

    suspend fun removeInfo(
        serviceId: Int,
        url: String,
        infoType: InfoItem.InfoType
    ) = mutex.withLock {
        cache.remove(keyOf(serviceId, url, infoType))
    }

    suspend fun clearCache() = mutex.withLock {
        cache.clear()
    }

    suspend fun trimCache() = mutex.withLock {
        removeStaleCache()
        cache.trimToSize(TRIM_CACHE_TO)
    }

    suspend fun size(): Long = mutex.withLock {
        cache.size().toLong()
    }

    private class CacheData(val info: Info, timeoutMillis: Long) {
        private val expireTimestamp: Long =
            Clock.System.now().toEpochMilliseconds() + timeoutMillis

        val isExpired: Boolean
            get() = Clock.System.now().toEpochMilliseconds() > expireTimestamp
    }

    companion object {
        val instance = InfoCache()
        private const val MAX_ITEMS_ON_CACHE = 60
        private const val TRIM_CACHE_TO = 30

        private fun keyOf(serviceId: Int, url: String, infoType: InfoItem.InfoType): String {
            return "$serviceId$url$infoType"
        }
    }

    private fun removeStaleCache() {
        for ((key, data) in cache.snapshot()) {
            if (data.isExpired) {
                cache.remove(key)
            }
        }
    }

    private fun getInfo(key: String): Info? {
        val data = cache.get(key) ?: return null
        if (data.isExpired) {
            cache.remove(key)
            return null
        }
        return data.info
    }
}
