package mottet.me.crawler.source

import mottet.me.crawler.now
import mottet.me.crawler.toReadableNumber
import org.jsoup.Jsoup
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.CrudRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/twitter")
class TwitterController(val repository: TwitterRepository) {
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
        lastUpdated = now()
    }

    @Scheduled(cron = "0 0 0 * * ?")
    fun saveMetric() {
        repository.save(Twitter(date = LocalDate.now(), favorites = favorites, followers = followers))
    }

    private fun fetchFavorite() = fetch("[data-nav='favorites'] .ProfileNav-value").toInt()
    private fun fetchFollowers() = fetch("[data-nav='followers'] .ProfileNav-value").toInt()
    private fun fetch(css: String) = Jsoup.connect("https://twitter.com/pupscan")
            .get()
            .select(css)
            .text()!!
}

@Document
class Twitter(@Id val id: String = UUID.randomUUID().toString(),
              @Indexed val date: LocalDate,
              val favorites: Int,
              val followers: Int)

interface TwitterRepository : CrudRepository<Twitter, String>