package mottet.me.crawler.source

import org.jsoup.Jsoup
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import org.springframework.data.repository.CrudRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/facebook")
class FacebookController(val facebookService : FacebookService) {

    @RequestMapping("/current")
    fun metrics() = "{" +
            "\"favorites\" : \"${facebookService.currentFavorites()}\", " +
            "\"followers\" : \"${facebookService.currentFallowers()}\", " +
            "\"lastUpdated\" : \"${facebookService.currentLastUpdated()}\"" +
            "}"

    @RequestMapping("/favorites")
    fun last7daysFavorites() = facebookService.last7daysFavorites()

}


@Service
class FacebookService(val repository: FacebookRepository) {
    private var favorites = 0
    private var followers = 0
    private var lastUpdated = LocalDateTime.now()

    fun currentFavorites() = favorites
    fun currentFallowers() = followers
    fun currentLastUpdated() = lastUpdated

    @Scheduled(fixedDelay = 700_000, initialDelay = 0)
    fun currentMetrics() {
        favorites = fetchFavorite()
        followers = fetchFollowers()
        lastUpdated = LocalDateTime.now()
    }

    fun last7daysFavorites() = (0L..6)
            .mapNotNull { repository.findOne(LocalDate.now().minusDays(it).toEpochDay()) }
            .map { it.favorites }
            .toList()

    @Scheduled(cron = "0 0 0 * * ?")
    fun saveMetric() {
        repository.save(Facebook(date = LocalDate.now().toEpochDay(), favorites = favorites, followers = followers))
    }

    private fun fetchFavorite() = fetch("div:eq(2)._2pi9._2pi2 ._4bl9  div").replace("[^\\d]".toRegex(), "").toInt()
    private fun fetchFollowers() = fetch("div:eq(3)._2pi9._2pi2 ._4bl9  div").replace("[^\\d]".toRegex(), "").toInt()
    private fun fetch(css: String) = Jsoup.connect("https://fr-fr.facebook.com/pupscan/")
            .get()
            .select(css)
            .text()!!
}

@RedisHash("facebook")
class Facebook(@Id val date: Long,
               @Indexed val favorites: Int,
               @Indexed val followers: Int)

interface FacebookRepository : CrudRepository<Facebook, Long> {
    fun findByDate(date: LocalDate): List<Facebook>
}