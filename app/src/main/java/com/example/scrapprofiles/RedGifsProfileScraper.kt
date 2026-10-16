package com.example.scrapprofiles

import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** RedGIFs uses its documented temporary-token flow and creator media endpoint when available. */
internal class RedGifsProfileScraper {
    fun scrape(profile: ScrapProfile): List<ScrapMedia> {
        val username = creatorName(profile.url)
        if (username.isBlank()) throw IllegalArgumentException("Could not determine the RedGIFs creator name from this profile URL.")
        val apiResults = runCatching { scrapeApi(profile, username) }.getOrDefault(emptyList())
        if (apiResults.isNotEmpty()) return apiResults

        // Public HTML fallback only; no attempt is made to bypass access controls.
        val page = PublicScraperSupport.get(profile.url)
        PublicScraperSupport.requireSuccess(page)
        val results = linkedMapOf<String, ScrapMedia>()
        PublicScraperSupport.htmlMedia(profile, page, setOf("og:video", "og:video:url", "contenturl", "video:src"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
        PublicScraperSupport.jsonMedia(page.body, profile, page.finalUrl,
            setOf("contentUrl", "videoUrl", "video_url", "fileUrl", "file_url", "src", "hd", "sd", "mp4"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
        if (results.isEmpty()) throw IllegalStateException("RedGIFs returned no creator media through its documented endpoint or public page. The profile may be unavailable or the API may have changed.")
        return results.values.take(MAX_RESULTS).toList()
    }

    private fun scrapeApi(profile: ScrapProfile, username: String): List<ScrapMedia> {
        val auth = PublicScraperSupport.get("https://api.redgifs.com/v2/auth/temporary", mapOf("Accept" to "application/json"))
        PublicScraperSupport.requireSuccess(auth)
        val token = JSONObject(auth.body).optString("token").ifBlank { JSONObject(auth.body).optString("access_token") }
        if (token.isBlank()) throw IllegalStateException("RedGIFs temporary API token was not returned.")
        val encoded = URLEncoder.encode(username, StandardCharsets.UTF_8.name())
        val results = linkedMapOf<String, ScrapMedia>()
        // Current community-maintained API clients use /v2/users/{name}/search.
        // Keep the older /gifs route as a compatibility fallback because this API is
        // undocumented/volatile and may differ between deployments.
        val endpoints = listOf(
            { pageNumber: Int -> "https://api.redgifs.com/v2/users/$encoded/search?count=80&order=recent&page=$pageNumber" },
            { pageNumber: Int -> "https://api.redgifs.com/v2/users/$encoded/gifs?count=80&page=$pageNumber" }
        )
        for (endpoint in endpoints) {
            var endpointWorked = false
            for (pageNumber in 1..MAX_PAGES) {
                val mediaPage = try {
                    PublicScraperSupport.get(
                        endpoint(pageNumber),
                        mapOf(
                            "Authorization" to "Bearer $token",
                            "Accept" to "application/json, text/plain, */*",
                            "Referer" to "https://www.redgifs.com/",
                            "Origin" to "https://www.redgifs.com"
                        )
                    )
                } catch (_: Exception) { break }
                if (mediaPage.status !in 200..299) break
                endpointWorked = true
                val root = JSONObject(mediaPage.body)
                val array = root.optJSONArray("gifs")
                    ?: root.optJSONObject("gifs")?.optJSONArray("gifs")
                    ?: root.optJSONArray("items")
                    ?: root.optJSONObject("user")?.optJSONArray("gifs")
                    ?: JSONArrayCompat.empty()
                if (array.length() == 0) break
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val urls = item.optJSONObject("urls") ?: JSONObject()
                    val direct = urls.optString("hd")
                        .ifBlank { urls.optString("sd") }
                        .ifBlank { urls.optString("file") }
                        .ifBlank { item.optString("file_url") }
                        .ifBlank { item.optString("url") }
                    val thumb = urls.optString("poster")
                        .ifBlank { urls.optString("thumbnail") }
                        .ifBlank { urls.optString("vthumbnail") }
                        .ifBlank { item.optString("poster") }
                    if (!PublicScraperSupport.isPlayableUrl(direct)) continue
                    val id = item.optString("id").ifBlank { item.optString("gifId") }
                    val title = item.optString("title").ifBlank { id.ifBlank { "RedGIFs media" } }
                    val pageUrl = item.optString("web_url").ifBlank {
                        item.optString("url").takeIf { it.contains("redgifs.com", true) }
                            ?: "https://www.redgifs.com/watch/$id"
                    }
                    val media = PublicScraperSupport.media(
                        profile, title, pageUrl, direct,
                        PublicScraperSupport.resolve("https://www.redgifs.com/", thumb)
                    )
                    results.putIfAbsent(media.mediaUrl.substringBefore('?'), media)
                }
                if (results.size >= MAX_RESULTS) break
                // Some API variants return only one page or omit pagination metadata.
                val pages = root.optInt("pages", root.optInt("totalPages", 0))
                if (pages > 0 && pageNumber >= pages) break
                if (pages == 0 && array.length() < 80) break
            }
            if (results.isNotEmpty()) break
            // Continue to the compatibility endpoint when the current route returned no media.
            if (endpointWorked) continue
        }
        return results.values.take(MAX_RESULTS).toList()
    }

    private fun creatorName(url: String): String {
        val path = try { java.net.URI(url).path.trim('/') } catch (_: Exception) { "" }
        val parts = path.split('/').filter(String::isNotBlank)
        return when {
            parts.size >= 2 && parts.first().lowercase() in setOf("users", "user", "creators", "creator") -> parts[1]
            parts.size == 1 && parts[0].lowercase() !in setOf("trending", "search", "watch", "gifs", "categories") -> parts[0]
            else -> ""
        }
    }

    private object JSONArrayCompat { fun empty() = org.json.JSONArray() }
    companion object { private const val MAX_RESULTS = 500; private const val MAX_PAGES = 12 }
}
