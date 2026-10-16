package com.example.scrapprofiles

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ScrapProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("scrap_profiles_v1", Context.MODE_PRIVATE)
    @Synchronized fun profiles(): List<ScrapProfile> = try { JSONArray(prefs.getString("profiles", "[]")).toProfileList() } catch (_: Exception) { emptyList() }
    @Synchronized fun media(profileId: String): List<ScrapMedia> = try {
        val a = JSONArray(prefs.getString("media_$profileId", "[]"))
        buildList { for (i in 0 until a.length()) try { val o = a.getJSONObject(i); add(ScrapMedia(o.optString("id"), profileId, o.optString("title"), o.optString("pageUrl"), o.optString("mediaUrl"), o.optString("thumbnailUrl"), o.optString("mimeType", "video/*"))) } catch (_: Exception) {} }
    } catch (_: Exception) { emptyList() }
    @Synchronized fun add(url: String, customName: String? = null, imageUrl: String = ""): ScrapProfile? {
        val normalized = url.trim().trimEnd('/')
        val site = ScrapSite.fromUrl(normalized) ?: return null
        if (profiles().any { it.url.trimEnd('/').equals(normalized, true) }) return null
        val uri = try { java.net.URI(normalized) } catch (_: Exception) { return null }
        if (uri.scheme !in listOf("http", "https")) return null
        val profile = ScrapProfile(java.util.UUID.randomUUID().toString(), site.id, normalized, customName?.takeIf { it.isNotBlank() } ?: (uri.path?.trim('/')?.substringAfterLast('/')?.ifBlank { site.name } ?: site.name), imageUrl, System.currentTimeMillis())
        saveProfiles(profiles() + profile)
        return profile
    }
    @Synchronized fun update(profile: ScrapProfile) = saveProfiles(profiles().map { if (it.id == profile.id) profile else it })
    @Synchronized fun remove(profile: ScrapProfile) { saveProfiles(profiles().filterNot { it.id == profile.id }); prefs.edit().remove("media_${profile.id}").apply() }
    @Synchronized fun saveMedia(profileId: String, items: List<ScrapMedia>) {
        val a = JSONArray()
        items.forEach { m -> a.put(JSONObject().apply { put("id", m.id); put("title", m.title); put("pageUrl", m.pageUrl); put("mediaUrl", m.mediaUrl); put("thumbnailUrl", m.thumbnailUrl); put("mimeType", m.mimeType) }) }
        prefs.edit().putString("media_$profileId", a.toString()).apply()
    }
    private fun saveProfiles(items: List<ScrapProfile>) { val a = JSONArray(); items.forEach { a.put(it.toJson()) }; prefs.edit().putString("profiles", a.toString()).apply() }
}
