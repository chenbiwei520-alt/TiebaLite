package com.huanchengfly.tieba.post.ui.page.photoview

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 超过这个体积就不自动加载原图。
 *
 * 参照 Matter（微博客户端）的 WeiboImageProbe：几 MB 的原图在手机网络上下载要十几秒，
 * 用户点开一张图却盯着转圈，还不如先给一张能看清的、再让他自己决定要不要那张大的。
 * */
const val MaxAutoDownloadBytes: Long = 10L * 1024L * 1024L

private const val ProbeTimeoutMillis = 3000
private const val ProbeUserAgent = "Mozilla/5.0 (Linux; Android 13; Mobile)"
private const val ProbeReferer = "https://tieba.baidu.com/"
private const val ProbeCacheSize = 500

/**
 * 量一张图有多大。
 *
 * 只发 HEAD，不取正文；量不到（服务端不支持、超时、非 2xx）就返回 -1，调用方按「未知」处理，
 * 也就是照旧自动加载——宁可多下一次，也不要因为量不出体积把图藏起来。
 * */
object ImageSizeProbe {

    private val cache = object : LinkedHashMap<String, Long>(ProbeCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > ProbeCacheSize
        }
    }

    /** 已量到的体积（字节）；没量过就是 -1。 */
    fun cachedSize(url: String): Long = synchronized(cache) { cache[url] ?: -1L }

    suspend fun probe(url: String): Long {
        if (url.isBlank()) return -1L
        cachedSize(url).let { if (it > 0) return it }

        val length = withContext(Dispatchers.IO) { requestContentLength(url) }
        if (length > 0) {
            synchronized(cache) { cache[url] = length }
        }
        return length
    }

    private fun requestContentLength(url: String): Long {
        return runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = ProbeTimeoutMillis
                readTimeout = ProbeTimeoutMillis
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", ProbeUserAgent)
                setRequestProperty("Referer", ProbeReferer)
            }
            try {
                val code = connection.responseCode
                if (code in 200..299) connection.contentLengthLong else -1L
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(-1L)
    }
}

/** 把字节数写成「2.4 MB」「860 KB」这样的文案。 */
fun formatImageSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    return if (kb >= 1024.0) {
        String.format(Locale.US, "%.1f MB", kb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}
