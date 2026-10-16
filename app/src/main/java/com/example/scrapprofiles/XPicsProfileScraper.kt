package com.example.scrapprofiles

/** XPics-specific public-page rules for profile/gallery cards and embedded media metadata. */
internal class XPicsProfileScraper {
    fun scrape(profile: ScrapProfile): List<ScrapMedia> {
        val page = PublicScraperSupport.get(profile.url)
        PublicScraperSupport.requireSuccess(page)
        val results = linkedMapOf<String, ScrapMedia>()
        PublicScraperSupport.htmlMedia(profile, page, setOf("og:video", "og:video:url", "contenturl", "video:src", "twitter:player:stream"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
        PublicScraperSupport.jsonMedia(page.body, profile, page.finalUrl,
            setOf("contentUrl", "videoUrl", "video_url", "mediaUrl", "media_url", "fileUrl", "file_url", "src", "mp4", "videoSrc", "video_src", "streamUrl", "file"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }

        // XPics layouts vary, so accept only same-host media-detail links with media-oriented path segments.
        val detailLinks = Regex("(?is)<a\\b([^>]+)>").findAll(page.body).mapNotNull { match ->
            val href = PublicScraperSupport.attr(match.groupValues[1], "href") ?: return@mapNotNull null
            val resolved = PublicScraperSupport.resolve(page.finalUrl, href)
            val path = try { java.net.URI(resolved).path.lowercase() } catch (_: Exception) { "" }
            val sameHost = try { java.net.URI(resolved).host?.endsWith("xpics.me", true) == true } catch (_: Exception) { false }
            if (sameHost && path != "/" && listOf("/video", "/videos", "/clip", "/media", "/gallery", "/photo").any(path::contains)) resolved else null
        }.distinct().take(MAX_DETAIL_PAGES).toList()
        detailLinks.forEach { url ->
            try {
                val detail = PublicScraperSupport.get(url)
                if (detail.status !in 200..299) return@forEach
                PublicScraperSupport.htmlMedia(profile, detail, setOf("og:video", "og:video:url", "contenturl", "video:src", "twitter:player:stream"))
                    .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
                PublicScraperSupport.jsonMedia(detail.body, profile, detail.finalUrl,
                    setOf("contentUrl", "videoUrl", "video_url", "mediaUrl", "media_url", "fileUrl", "file_url", "src", "mp4", "videoSrc", "video_src", "streamUrl", "file"))
                    .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
            } catch (_: Exception) { }
        }
        if (results.isEmpty()) throw IllegalStateException("XPics did not expose playable public media for this profile. Verify the profile URL and public availability.")
        return results.values.take(MAX_RESULTS).toList()
    }

    companion object { private const val MAX_DETAIL_PAGES = 24; private const val MAX_RESULTS = 500 }
}
