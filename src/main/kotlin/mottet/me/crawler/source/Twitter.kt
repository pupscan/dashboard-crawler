package mottet.me.crawler.source

import mottet.me.crawler.now
import org.jsoup.Jsoup
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TwitterController {
    private var favorites = 0
    private var followers = 0
    private var lastUpdated = now()

    @RequestMapping("/twitter")
    fun metrics() = "{" +
            "\"favorites\" : \"$favorites\", " +
            "\"followers\" : \"$followers\", " +
            "\"lastUpdated\" : \"$lastUpdated\"" +
            "}"

    @Scheduled(fixedDelay = 700_000, initialDelay = 0)
    fun fetch() {
        favorites = fetchFavorite()
        followers = fetchFollowers()
        lastUpdated = now()
    }

    private fun fetchFavorite() = fetch("[data-nav='favorites'] .ProfileNav-value").toInt()
    private fun fetchFollowers() = fetch("[data-nav='followers'] .ProfileNav-value").toInt()
    private fun fetch(css: String) = Jsoup.connect("https://twitter.com/pupscan")
            .get()
            .select(css)
            .text()!!
}

