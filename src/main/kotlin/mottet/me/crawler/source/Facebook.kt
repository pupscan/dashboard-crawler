package mottet.me.crawler.source

import mottet.me.crawler.now
import mottet.me.crawler.toReadableNumber
import org.jsoup.Jsoup
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/facebook")
class FacebookController {
    private var favorites = 0
    private var followers = 0
    private var lastUpdated = now()

    @RequestMapping("/favorites")
    fun favorite() = "{\"current\" : \"${favorites.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @RequestMapping("/followers")
    fun followers() = "{\"current\" : \"${followers.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @Scheduled(fixedDelay = 700_000, initialDelay = 0)
    fun fetch() {
        favorites = fetchFavorite()
        followers = fetchFollowers()
        lastUpdated =  now()
    }

    private fun fetchFavorite() = fetch("div:eq(2)._2pi9._2pi2 ._4bl9  div").replace("[^\\d]".toRegex(), "").toInt()
    private fun fetchFollowers() = fetch("div:eq(3)._2pi9._2pi2 ._4bl9  div").replace("[^\\d]".toRegex(), "").toInt()
    private fun fetch(css: String) = Jsoup.connect("https://fr-fr.facebook.com/pupscan/")
            .get()
            .select(css)
            .text()!!
}

