package com.yushosei.newpipe.util

import com.yushosei.newpipe.extractor.ServiceList
import com.yushosei.newpipe.extractor.StreamingService

object ServiceHelper {
    private val DEFAULT_FALLBACK_SERVICE: StreamingService = ServiceList.YouTube

    fun getCacheExpirationMillis(serviceId: Int): Long {
        return if (false) {
            5 * 60 * 1000L // 5분 = 300,000밀리초
        } else {
            1 * 60 * 60 * 1000L // 1시간 = 3,600,000밀리초
        }
    }
}
