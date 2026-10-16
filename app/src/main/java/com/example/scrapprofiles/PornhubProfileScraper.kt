package com.example.scrapprofiles

import java.util.Locale

/** Pornhub-specific public page rules: JSON-LD, video meta tags, and view_video.php card links. */
internal class PornhubProfileScraper {
    fun scrape(profile: ScrapProfile): List<ScrapMedia> {
        val page = PublicScraperSupport.get(profile.url)
        PublicScraperSupport.requireSuccess(page)
        val results = linkedMapOf<String, ScrapMedia>()
        PublicScraperSupport.htmlMedia(profile, page, setOf("og:video", "og:video:url", "og:video:secure_url", "contenturl", "video:src"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
        val json = PublicScraperSupport.jsonMedia(page.body, profile, page.finalUrl,
            setOf("contentUrl", "videoUrl", "video_url", "mediaUrl", "media_url", "src", "fileUrl", "file_url", "hd", "sd"))
        json.forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
        // Some creator pages expose only video cards; inspect a bounded number of public video pages.
        val links = Regex("(?is)<a\\b([^>]+)>").findAll(page.body).mapNotNull { match ->
            val attrs = match.groupValues[1]
            val href = PublicScraperSupport.attr(attrs, "href") ?: return@mapNotNull null
            val resolved = PublicScraperSupport.resolve(page.finalUrl, href)
            if (resolved.contains("pornhub.com/view_video.php", true) && resolved.contains("viewkey=", true)) resolved else null
        }.distinct().take(MAX_VIDEO_PAGES).toList()
        links.forEach { videoPageUrl ->
            try {
                val videoPage = PublicScraperSupport.get(videoPageUrl)
                if (videoPage.status !in 200..299) return@forEach
                val fromHtml = PublicScraperSupport.htmlMedia(profile, videoPage, setOf("og:video", "og:video:url", "og:video:secure_url", "contenturl", "video:src"))
                fromHtml.forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
                PublicScraperSupport.jsonMedia(videoPage.body, profile, videoPage.finalUrl,
                    setOf("contentUrl", "videoUrl", "video_url", "mediaUrl", "media_url", "src", "fileUrl", "file_url", "quality_720p", "quality_480p"))
                    .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
            } catch (_: Exception) { /* One unavailable card must not fail the entire profile. */ }
        }
        if (results.isEmpty()) throw IllegalStateException("Pornhub did not expose playable public media on this profile. Try refreshing later or verify that the profile is publicly accessible.")
        return results.values.take(MAX_RESULTS).toList()
    }

    companion object { private const val MAX_VIDEO_PAGES = 24; private const val MAX_RESULTS = 500 }
}
