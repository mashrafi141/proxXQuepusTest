package com.example.scrapprofiles

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class ScrapSite(val id: String, val name: String, val host: String) {
    companion object {
        val supported = listOf(
            ScrapSite("pornhub", "Pornhub", "pornhub.com"),
            ScrapSite("xhamster", "xHamster", "xhamster.com"),
            ScrapSite("redgifs", "RedGIFs", "redgifs.com"),
            ScrapSite("xxxfollow", "XXXFollow", "xxxfollow.com"),
            ScrapSite("xpics", "XPics", "xpics.me")
        )
        fun fromUrl(value: String): ScrapSite? {
            return try {
                val host = URI(value.trim()).host?.lowercase()?.removePrefix("www.") ?: return null
                supported.firstOrNull { host == it.host || host.endsWith(".${it.host}") }
            } catch (_: Exception) { null }
        }
    }
}

data class ScrapProfile(
    val id: String,
    val siteId: String,
    val url: String,
    val name: String,
    val imageUrl: String = "",
    val updatedAt: Long = 0L
)

data class ScrapMedia(
    val id: String,
    val profileId: String,
    val title: String,
    val pageUrl: String,
    val mediaUrl: String,
    val thumbnailUrl: String = "",
    val mimeType: String = "video/*"
)

internal fun JSONArray.toProfileList(): List<ScrapProfile> = buildList {
    for (i in 0 until length()) try {
        val o = getJSONObject(i)
        add(ScrapProfile(o.optString("id"), o.optString("siteId"), o.optString("url"), o.optString("name"), o.optString("imageUrl"), o.optLong("updatedAt")))
    } catch (_: Exception) { }
}

internal fun ScrapProfile.toJson() = JSONObject().apply {
    put("id", id); put("siteId", siteId); put("url", url); put("name", name); put("imageUrl", imageUrl); put("updatedAt", updatedAt)
}
