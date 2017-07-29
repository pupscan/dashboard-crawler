package mottet.me.crawler.source

import facebook4j.FacebookFactory
import mottet.me.crawler.safeDisplaySecret
import mottet.me.crawler.toReadableDate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
class FacebookService(@Value("\${facebook.app-id}") val facebookAppId: String,
                      @Value("\${facebook.app-secret}") val facebookAppSecret: String,
                      val repository: FacebookRepository) {
    private val logger = LoggerFactory.getLogger(FacebookService::class.java)!!

    private var favorites = 0
    private var followers = 0
    private var lastUpdated = LocalDateTime.now()!!
    private val facebook = FacebookFactory().instance.apply {
        logger.info("Connect to Facebook with appId=$facebookAppId, appSecret=${facebookAppSecret.safeDisplaySecret()}")
        setOAuthAppId(facebookAppId, facebookAppSecret)
        oAuthAccessToken = oAuthAppAccessToken
    }!!

    fun currentFavorites() = favorites
    fun currentFollowers() = followers
    fun lastUpdateDateTime() = lastUpdated
    fun last30days() = repository.findTop30ByOrderByDateDesc().map { it.favorites } + currentFavorites()

    @Scheduled(fixedDelay = 700_000, initialDelay = 0)
    fun fetch() {
        favorites = facebook.callGetAPI("v2.10/1757878341134060", mapOf("fields" to "fan_count"))
                .asJSONObject()
                .getInt("fan_count")
        followers = favorites
        lastUpdated = LocalDateTime.now()
    }

    @Scheduled(cron = "0 59 23 * * ?") // every night at 23h59
    fun saveFaceBookData() {
        repository.save(Facebook(date = LocalDate.now(), favorites = favorites, followers = followers))
    }

}

@Document
class Facebook(@Id val id: String = UUID.randomUUID().toString(),
               @Indexed val date: LocalDate,
               val favorites: Int,
               val followers: Int)

interface FacebookRepository : CrudRepository<Facebook, String> {
    fun findTop30ByOrderByDateDesc(): List<Facebook>
}