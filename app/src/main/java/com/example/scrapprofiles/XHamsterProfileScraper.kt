package com.example.scrapprofiles

/** xHamster-specific rules for public creator pages and embedded player/JSON metadata. */
internal class XHamsterProfileScraper {
    fun scrape(profile: ScrapProfile): List<ScrapMedia> {
        val page = PublicScraperSupport.get(profile.url)
        PublicScraperSupport.requireSuccess(page)
        val results = linkedMapOf<String, ScrapMedia>()
        PublicScraperSupport.htmlMedia(profile, page, setOf("og:video", "og:video:url", "contenturl", "video:src"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
        PublicScraperSupport.jsonMedia(page.body, profile, page.finalUrl,
            setOf("contentUrl", "videoUrl", "video_url", "fileUrl", "file_url", "src", "hls", "mp4", "hd", "sd", "url_720p", "url_480p"))
            .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }

        // Creator pages generally link to /videos/<slug>; inspect only a small page window.
        val links = Regex("(?is)<a\\b([^>]+)>").findAll(page.body).mapNotNull { match ->
            val href = PublicScraperSupport.attr(match.groupValues[1], "href") ?: return@mapNotNull null
            val resolved = PublicScraperSupport.resolve(page.finalUrl, href)
            if (Regex("(?i)^https?://(?:www\\.)?xhamster\\.com/videos/[^?#]+") .containsMatchIn(resolved)) resolved else null
        }.distinct().take(MAX_VIDEO_PAGES).toList()
        links.forEach { url ->
            try {
                val videoPage = PublicScraperSupport.get(url)
                if (videoPage.status !in 200..299) return@forEach
                PublicScraperSupport.htmlMedia(profile, videoPage, setOf("og:video", "og:video:url", "contenturl", "video:src"))
                    .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
                PublicScraperSupport.jsonMedia(videoPage.body, profile, videoPage.finalUrl,
                    setOf("contentUrl", "videoUrl", "video_url", "fileUrl", "file_url", "src", "hls", "mp4", "hd", "sd", "url_720p", "url_480p"))
                    .forEach { results.putIfAbsent(it.mediaUrl.substringBefore('?'), it) }
            } catch (_: Exception) { }
        }
        if (results.isEmpty()) throw IllegalStateException("xHamster did not expose playable public media. The profile may require client-side rendering or restricted access.")
        return results.values.take(MAX_RESULTS).toList()
    }

    companion object { private const val MAX_VIDEO_PAGES = 24; private const val MAX_RESULTS = 500 }
}
