package mottet.me.crawler.source

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
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/facebook")
class FacebookController(val repository: FacebookRepository) {
    private var favorites = 0
    private var followers = 0
    private var lastUpdated = LocalDateTime.now()!!

    @RequestMapping("/favorites")
    fun favorite() = "{\"current\" : \"${favorites.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @RequestMapping("/followers")
    fun followers() = "{\"current\" : \"${followers.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\" }"

    fun currentFavorites() = favorites
    fun currentFollowers() = followers
    fun currentLastUpdated() = lastUpdated

    @Scheduled(fixedDelay = 700_000, initialDelay = 0)
    fun fetch() {
        favorites = fetchFavorite()
        followers = fetchFollowers()
        lastUpdated = LocalDateTime.now()
    }

    @Scheduled(cron = "0 0 0 * * ?")
    fun saveMetric() {
        repository.save(Facebook(date = LocalDate.now(), favorites = favorites, followers = followers))
    }

    private fun fetchFavorite() = fetch("div:eq(2)._2pi9._2pi2 ._4bl9  div").replace("[^\\d]".toRegex(), "").toInt()
    private fun fetchFollowers() = fetch("div:eq(3)._2pi9._2pi2 ._4bl9  div").replace("[^\\d]".toRegex(), "").toInt()
    private fun fetch(css: String) = Jsoup.connect("https://fr-fr.facebook.com/pupscan/")
            .get()
            .select(css)
            .text()!!
}

@Document
class Facebook(@Id val id: String = UUID.randomUUID().toString(),
               @Indexed val date: LocalDate,
               val favorites: Int,
               val followers: Int)

interface FacebookRepository : CrudRepository<Facebook, String>