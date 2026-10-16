package com.example.scrapprofiles

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Shared transport/parsing helpers. Platform-specific extraction rules live in separate adapters. */
internal object PublicScraperSupport {
    data class Page(val requestedUrl: String, val finalUrl: String, val status: Int, val body: String, val contentType: String)

    fun get(url: String, headers: Map<String, String> = emptyMap()): Page {
        require(url.startsWith("https://") || url.startsWith("http://")) { "Only HTTP(S) URLs are supported" }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 18_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,video/*;q=0.8,*/*;q=0.7")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            setRequestProperty("Cache-Control", "no-cache")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
                val out = StringBuilder()
                val buffer = CharArray(8192)
                var total = 0
                while (total < MAX_BODY_CHARS) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, MAX_BODY_CHARS - total))
                    if (count <= 0) break
                    out.append(buffer, 0, count)
                    total += count
                }
                out.toString()
            }.orEmpty()
            return Page(url, connection.url.toString(), status, body, connection.contentType.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    fun requireSuccess(page: Page) {
        if (page.status !in 200..299) throw IllegalStateException("Source returned HTTP ${page.status}; it may be unavailable or restrict automated access.")
    }

    fun resolve(base: String, raw: String?): String {
        val value = raw.orEmpty().trim().replace("\\/", "/").replace("\\u0026", "&").replace("&amp;", "&")
        if (value.isBlank() || value.startsWith("data:") || value.startsWith("javascript:")) return ""
        return try { URL(URL(base), value).toString() } catch (_: Exception) { "" }
    }

    fun attr(attrs: String, key: String): String? = Regex("(?is)(?:^|\\s)${Regex.escape(key)}\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))")
        .find(attrs)?.let { it.groupValues.drop(1).firstOrNull(String::isNotBlank) }?.let(::decodeHtml)

    fun decodeHtml(value: String): String = value
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("\\u002F", "/").replace("\\u0026", "&")

    fun clean(value: String): String = decodeHtml(value).replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

    fun isPlayableUrl(value: String): Boolean {
        val v = value.lowercase(Locale.ROOT)
        if (!v.startsWith("http://") && !v.startsWith("https://")) return false
        if (v.contains(".m3u8") || v.contains(".mpd")) return true
        return listOf(".mp4", ".webm", ".m4v", ".mov", ".mkv", ".m3u8", ".mpd", "video_url", "videoplayback").any(v::contains)
    }

    fun media(profile: ScrapProfile, title: String, pageUrl: String, mediaUrl: String, thumbnail: String, mime: String = "video/*"): ScrapMedia {
        val key = mediaUrl.substringBefore('?')
        return ScrapMedia(key.hashCode().toString(), profile.id, clean(title).ifBlank { "Video" }, pageUrl, mediaUrl, thumbnail, mime)
    }

    /** Extract direct media from JSON objects without treating ordinary profile/video page URLs as streams. */
    fun jsonMedia(body: String, profile: ScrapProfile, baseUrl: String, siteFields: Set<String>): List<ScrapMedia> {
        val result = linkedMapOf<String, ScrapMedia>()
        fun walk(value: Any?) {
            when (value) {
                is JSONObject -> {
                    val title = value.optString("title", value.optString("name", value.optString("headline", "Video")))
                    val page = resolve(baseUrl, value.optString("web_url", value.optString("pageUrl", value.optString("permalink", value.optString("url", baseUrl)))))
                    val thumb = resolve(baseUrl, value.optString("poster", value.optString("thumbnail", value.optString("thumbnailUrl", value.optString("image", "")))))
                    for (field in siteFields) {
                        val raw = value.optString(field, "")
                        val mediaUrl = resolve(baseUrl, raw)
                        if (isPlayableUrl(mediaUrl) || isExplicitMediaField(field, mediaUrl)) {
                            result.putIfAbsent(mediaUrl.substringBefore('?'), media(profile, title, page.ifBlank { baseUrl }, mediaUrl, thumb))
                        }
                    }
                    value.keys().forEach { walk(value.opt(it)) }
                }
                is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i))
                is String -> {
                    val s = value.trim()
                    if (s.startsWith("{")) try { walk(JSONObject(s)) } catch (_: Exception) { }
                    else if (s.startsWith("[")) try { walk(JSONArray(s)) } catch (_: Exception) { }
                }
            }
        }
        try { walk(if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body)) } catch (_: Exception) { }
        // Many sites place JSON objects inside HTML script tags rather than returning JSON documents.
        // Scan only known media fields for URL-like values; normal page links are filtered out above.
        for (field in siteFields) {
            val pattern = Regex("""(?is)["']${Regex.escape(field)}["']\s*:\s*["']([^"']{5,2048})["']""")
            pattern.findAll(body).forEach { match ->
                val mediaUrl = resolve(baseUrl, match.groupValues[1])
                if (!isPlayableUrl(mediaUrl) && !isExplicitMediaField(field, mediaUrl)) return@forEach
                val start = (match.range.first - 1800).coerceAtLeast(0)
                val context = body.substring(start, match.range.first)
                val title = Regex("""(?is)["'](?:title|name|headline)["']\s*:\s*["']([^"']{1,240})["']""")
                    .findAll(context).lastOrNull()?.groupValues?.get(1)?.let(::decodeHtml).orEmpty().ifBlank { "Video" }
                val thumb = Regex("""(?is)["'](?:thumbnailUrl|thumbnail|poster|image)["']\s*:\s*["']([^"']{5,2048})["']""")
                    .findAll(context).lastOrNull()?.groupValues?.get(1).orEmpty()
                result.putIfAbsent(mediaUrl.substringBefore('?'), media(profile, title, baseUrl, mediaUrl, resolve(baseUrl, thumb)))
            }
        }
        return result.values.toList()
    }

    fun htmlMedia(profile: ScrapProfile, page: Page, siteFields: Set<String>): List<ScrapMedia> {
        val body = page.body
        var title = profile.name
        var thumbnail = profile.imageUrl
        Regex("(?is)<meta\\b([^>]+)>").findAll(body).forEach { match ->
            val attrs = match.groupValues[1]
            val key = (attr(attrs, "property") ?: attr(attrs, "name")).orEmpty().lowercase(Locale.ROOT)
            val value = attr(attrs, "content").orEmpty()
            when (key) {
                "og:title", "twitter:title" -> if (value.isNotBlank()) title = value
                "og:image", "twitter:image", "og:image:url" -> if (thumbnail.isBlank() && value.isNotBlank()) thumbnail = resolve(page.finalUrl, value)
            }
        }
        val result = linkedMapOf<String, ScrapMedia>()
        fun add(raw: String, t: String = title, thumb: String = thumbnail, explicitMedia: Boolean = false) {
            val url = resolve(page.finalUrl, raw)
            if (!isPlayableUrl(url) && !(explicitMedia && isHttpUrl(url) && !isPageLikeUrl(url))) return
            result.putIfAbsent(url.substringBefore('?'), media(profile, t, page.finalUrl, url, resolve(page.finalUrl, thumb)))
        }
        Regex("(?is)<video\\b([^>]*)>(.*?)</video>|<source\\b([^>]*)/?>").findAll(body).forEach { m ->
            val attrs = m.groupValues[1] + " " + m.groupValues[3]
            val src = attr(attrs, "src") ?: Regex("(?is)<source[^>]+src=['\"]([^'\"]+)").find(m.groupValues[2])?.groupValues?.get(1)
            if (src != null) add(src, thumb = attr(attrs, "poster") ?: thumbnail)
        }
        Regex("(?is)<meta\\b([^>]+)>").findAll(body).forEach { m ->
            val attrs = m.groupValues[1]
            val key = (attr(attrs, "property") ?: attr(attrs, "name") ?: attr(attrs, "itemprop")).orEmpty().lowercase(Locale.ROOT)
            if (siteFields.any { it.equals(key, ignoreCase = true) }) attr(attrs, "content")?.let { add(it, explicitMedia = true) }
        }
        val jsonLd = Regex("(?is)<script[^>]+type=['\"]application/ld\\+json['\"][^>]*>(.*?)</script>").findAll(body)
        jsonLd.forEach { match ->
            val parsed = jsonMedia(match.groupValues[1], profile, page.finalUrl, setOf("contentUrl", "embedUrl", "videoUrl", "src", "fileUrl"))
            parsed.forEach { result.putIfAbsent(it.mediaUrl.substringBefore('?'), it.copy(profileId = profile.id)) }
        }
        return result.values.toList()
    }

    private fun isExplicitMediaField(field: String, url: String): Boolean {
        val key = field.lowercase(Locale.ROOT).replace("-", "_")
        val explicit = key in setOf(
            "contenturl", "content_url", "videourl", "video_url", "mediaurl", "media_url",
            "fileurl", "file_url", "streamurl", "stream_url", "video:src", "og:video",
            "og:video:url", "og:video:secure_url", "twitter:player:stream", "hls", "mp4", "hd", "sd"
        )
        return explicit && isHttpUrl(url) && !isPageLikeUrl(url)
    }

    private fun isHttpUrl(value: String): Boolean = value.startsWith("https://", true) || value.startsWith("http://", true)

    /** Reject creator/watch pages while allowing signed CDN stream URLs without file extensions. */
    private fun isPageLikeUrl(value: String): Boolean {
        val uri = try { URI(value) } catch (_: Exception) { return true }
        val path = uri.path.orEmpty().lowercase(Locale.ROOT)
        if (path.isBlank() || path == "/") return true
        return listOf("/users/", "/user/", "/creators/", "/creator/", "/profile/", "/profiles/", "/watch/", "/videos/", "/video/", "/posts/", "/post/", "/search").any(path::contains)
    }

    fun profileName(profile: ScrapProfile, fallback: String): String = profile.name.takeIf { it.isNotBlank() } ?: fallback

    fun usernameFromPath(url: String): String = try {
        URI(url).path.trim('/').substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }.orEmpty()
    } catch (_: Exception) { "" }

    fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private const val MAX_BODY_CHARS = 8_000_000
}
