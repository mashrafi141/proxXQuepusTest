package com.example.scrapprofiles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Routes each saved profile to an isolated, site-specific public metadata adapter. */
class ScrapProfileScraper {
    private val pornhub = PornhubProfileScraper()
    private val xhamster = XHamsterProfileScraper()
    private val redgifs = RedGifsProfileScraper()
    private val xxxfollow = XXXFollowProfileScraper()
    private val xpics = XPicsProfileScraper()

    suspend fun scrape(profile: ScrapProfile): Result<List<ScrapMedia>> = withContext(Dispatchers.IO) {
        runCatching {
            when (profile.siteId.lowercase()) {
                "pornhub" -> pornhub.scrape(profile)
                "xhamster" -> xhamster.scrape(profile)
                "redgifs" -> redgifs.scrape(profile)
                "xxxfollow" -> xxxfollow.scrape(profile)
                "xpics" -> xpics.scrape(profile)
                else -> throw IllegalArgumentException("No scraping adapter is registered for site '${profile.siteId}'.")
            }.distinctBy { it.mediaUrl.substringBefore('?') }
        }
    }
}
