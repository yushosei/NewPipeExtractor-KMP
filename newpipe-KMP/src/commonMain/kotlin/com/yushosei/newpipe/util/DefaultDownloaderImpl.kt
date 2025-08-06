/*
 * Multiplatform replacement for the original OkHttp‑based DownloaderImpl.
 * Uses Ktor’s multiplatform HttpClient so it works on Android, JVM‑desktop,
 * iOS/Darwin, and (with the CIO/JS engine) web targets that Compose
 * Multiplatform supports.
 *
 * Put this file in commonMain (e.g. base/newpipe/downloader) and make sure
 * the following dependencies are available in all source‑sets:
 *
 *   implementation("io.ktor:ktor-client-core:2.3.8")          // or newer
 *   implementation("io.ktor:ktor-client-cio:2.3.8")           // JVM/Android
 *   implementation("io.ktor:ktor-client-darwin:2.3.8")        // iOS
 *   implementation("io.ktor:ktor-client-js:2.3.8")            // JS, if needed
 *   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
 *
 * No other changes are required in NewPipe‑Extractor — the public behaviour
 * (blocking API, exception types, header/cookie handling, etc.) is identical.
 */

package com.yushosei.newpipe.util

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.downloader.Request
import com.yushosei.newpipe.extractor.downloader.Response
import com.yushosei.newpipe.extractor.exceptions.ReCaptchaException
import kotlin.collections.set

class DefaultDownloaderImpl(
    /**
     * A fully configured HttpClient can be injected for tests or
     * engine‑specific tuning.  When omitted we create a 30‑second‑timeout
     * client that works on every Compose Multiplatform target.
     */
    private val client: HttpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        expectSuccess = false               // we handle ≥400 ourselves
    }
) : Downloader() {

    /* ----------------------------------------------------------- cookies */

    private val cookies: MutableMap<String, String> = mutableMapOf()

    /** Aggregate the cookies relevant for `url` into a single "Cookie" header. */
    fun getCookies(url: String): String {
        val candidate = buildList<String> {
            if (url.contains(YOUTUBE_DOMAIN)) {
                getCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY)?.let(::add)
            }
            getCookie(RECAPTCHA_COOKIES_KEY)?.let(::add)
        }
        return candidate
            .flatMap { it.split(";").map(String::trim) }
            .distinct()
            .joinToString("; ")
    }

    fun getCookie(key: String): String? = cookies[key]

    fun setCookie(key: String, value: String) {
        cookies[key] = value
    }

    fun removeCookie(key: String) {
        cookies.remove(key)
    }

    /**
     * Toggle YouTube’s "restricted mode" cookie and clear extractor cache so
     * NewPipe re‑parses pages with the new preference.
     */
    suspend fun updateYoutubeRestrictedModeCookies(enabled: Boolean) {
        if (enabled) {
            setCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY, YOUTUBE_RESTRICTED_MODE_COOKIE)
        } else {
            removeCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY)
        }
        ExtractorHelper.clearCache()
    }

    /* ---------------------------------------------------- HEAD convenience */

    @Throws(Exception::class)
    fun getContentLength(url: String?): Long {
        if (url == null) throw Exception("URL is null")
        return try {
            runBlocking {
                val resp: HttpResponse = client.head(url)
                resp.headers[HttpHeaders.ContentLength]?.toLong()
                    ?: throw Exception("Missing Content‑Length header")
            }
        } catch (e: NumberFormatException) {
            throw Exception("Invalid content length", e)
        } catch (e: ReCaptchaException) {
            throw Exception(e)
        }
    }

    /* ------------------------------------------------------------- main IO */
    override fun execute(request: Request): Response = runBlocking {
        // ─── build request ────────────────────────────────────────────────
        val httpResponse: HttpResponse = client.request {
            url(request.url())
            method = HttpMethod(parseMethod(request.httpMethod()))
            // User‑agent + collected cookies
            headers {
                append(HttpHeaders.UserAgent, USER_AGENT)
                val cookieHeader = getCookies(request.url())
                if (cookieHeader.isNotEmpty()) append(HttpHeaders.Cookie, cookieHeader)

                // Forward the caller’s headers (support multi‑value)
                request.headers().forEach { (name, values) ->
                    if (values.size == 1) {
                        set(name, values.first())
                    } else {
                        remove(name)
                        values.forEach { append(name, it) }
                    }
                }
            }

            // Body (if any)
            request.dataToSend()?.let { setBody(it) }   // ByteArray -> streamed
        }

        // ─── handle rate‑limit / captcha ──────────────────────────────────
        if (httpResponse.status.value == 429) {
            httpResponse.cancel()
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }

        // ─── build extractor Response ─────────────────────────────────────
        val headersMap: Map<String, List<String>> =
            httpResponse.headers.names()
                .associateWith { httpResponse.headers.getAll(it) ?: emptyList() }

        val response = Response(
            httpResponse.status.value,
            httpResponse.status.description,
            headersMap,
            httpResponse.bodyAsText(),
            httpResponse.request.url.toString()
        )
        httpResponse.cancel()
        response
    }

    /* ----------------------------------------------------------- helpers  */

    private fun parseMethod(name: String): String =
        when (name.uppercase()) {
            "GET" -> "GET"
            "POST" -> "POST"
            "PUT" -> "PUT"
            "PATCH" -> "PATCH"
            "HEAD" -> "HEAD"
            "DELETE" -> "DELETE"
            else -> error("Unsupported HTTP method: $name")
        }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0"
        const val YOUTUBE_RESTRICTED_MODE_COOKIE_KEY = "youtube_restricted_mode_key"
        const val YOUTUBE_RESTRICTED_MODE_COOKIE = "PREF=f2=8000000"
        const val YOUTUBE_DOMAIN = "youtube.com"
        const val RECAPTCHA_COOKIES_KEY = "recaptcha_cookies"

        suspend fun initDefault(): DefaultDownloaderImpl {
            return DefaultDownloaderImpl().apply {
                setCookie(RECAPTCHA_COOKIES_KEY, "")
                updateYoutubeRestrictedModeCookies(true)
            }
        }
    }
}
