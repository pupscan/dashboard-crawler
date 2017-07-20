package mottet.me.crawler.source

import mottet.me.crawler.toReadableDate
import mottet.me.crawler.toReadableNumber
import org.jsoup.Jsoup
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.CrudRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/facebook")
class FacebookController(val service: FacebookService) {

    @RequestMapping("/favorites")
    fun favorite() = "{\"current\" : ${service.currentFavorites()}, \"lastUpdated\" : " +
            "\"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/followers")
    fun followers() = "{\"current\" : ${service.currentFollowers()}, \"lastUpdated\" : " +
            "\"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/last")
    fun last30Days() = service.last30days()
}

@Service
class FacebookService(val repository: FacebookRepository) {
    private var favorites = 0
    private var followers = 0
    private var lastUpdated = LocalDateTime.now()!!

    fun currentFavorites() = fetch("div:eq(2)._2pi9._2pi2 ._4bl9  div").replace("[^\\d]".toRegex(), "").toInt()
    fun currentFollowers() = fetch("div:eq(3)._2pi9._2pi2 ._4bl9  div").replace("[^\\d]".toRegex(), "").toInt()
    fun lastUpdateDateTime() = lastUpdated
    fun last30days() = repository.findTop30ByOrderByDateDesc().map { it.favorites } + currentFavorites()

    @Scheduled(fixedDelay = 700_000, initialDelay = 0)
    fun fetch() {
        favorites = currentFavorites()
        followers = currentFollowers()
        lastUpdated = LocalDateTime.now()
    }

    @Scheduled(cron = "0 59 23 * * ?") // every night at 23h59
    fun saveFaceBookData() {
        repository.save(Facebook(date = LocalDate.now(), favorites = favorites, followers = followers))
    }

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

interface FacebookRepository : CrudRepository<Facebook, String> {
    fun findTop30ByOrderByDateDesc(): List<Facebook>
}