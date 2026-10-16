package com.example.scrapprofiles

/** XXXFollow-specific public-page rules for post cards and embedded video-player metadata. */
internal class XXXFollowProfileScraper {
    fun scrape(profile: ScrapProfile): List<ScrapMedia> {
        val page = PublicScraperSupport.get(profile.url)
        PublicScraperSupport.requireSuccess(page)
        val results = linkedMapOf<String, ScrapMedia>()
        PublicScraperSupport.htmlMedia(profile, page, setOf("og:video", "og:video:url", "contenturl", "video:src", "twitter:player:stream"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
        PublicScraperSupport.jsonMedia(page.body, profile, page.finalUrl,
            setOf("contentUrl", "videoUrl", "video_url", "mediaUrl", "media_url", "fileUrl", "file_url", "src", "mp4", "videoSrc", "video_src", "streamUrl"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }

        val postLinks = Regex("(?is)<a\\b([^>]+)>").findAll(page.body).mapNotNull { match ->
            val href = PublicScraperSupport.attr(match.groupValues[1], "href") ?: return@mapNotNull null
            val resolved = PublicScraperSupport.resolve(page.finalUrl, href)
            val path = try { java.net.URI(resolved).path.lowercase() } catch (_: Exception) { "" }
            if (try { java.net.URI(resolved).host?.endsWith("xxxfollow.com", true) == true } catch (_: Exception) { false } &&
                path != "/" && (path.contains("/video") || path.contains("/post") || path.contains("/watch") || path.contains("/clip") || path.contains("/media"))) resolved else null
        }.distinct().take(MAX_POST_PAGES).toList()
        postLinks.forEach { url ->
            try {
                val videoPage = PublicScraperSupport.get(url)
                if (videoPage.status !in 200..299) return@forEach
                PublicScraperSupport.htmlMedia(profile, videoPage, setOf("og:video", "og:video:url", "contenturl", "video:src", "twitter:player:stream"))
                    .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
                PublicScraperSupport.jsonMedia(videoPage.body, profile, videoPage.finalUrl,
                    setOf("contentUrl", "videoUrl", "video_url", "mediaUrl", "media_url", "fileUrl", "file_url", "src", "mp4", "videoSrc", "video_src", "streamUrl"))
                    .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
            } catch (_: Exception) { }
        }
        if (results.isEmpty()) throw IllegalStateException("XXXFollow did not expose playable public media for this profile. Client-rendered or restricted media cannot be extracted by this public-page adapter.")
        return results.values.take(MAX_RESULTS).toList()
    }

    companion object { private const val MAX_POST_PAGES = 24; private const val MAX_RESULTS = 500 }
}
