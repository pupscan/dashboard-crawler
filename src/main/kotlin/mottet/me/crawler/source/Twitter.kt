package mottet.me.crawler.source

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
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/twitter")
class TwitterController(val service: TwitterService) {

    @RequestMapping("/favorites")
    fun favorite() = "{\"current\" : ${service.currentLikes()}, \"lastUpdated\" : " +
            "\"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/followers")
    fun followers() = "{\"current\" : ${service.currentFollowers()}, \"lastUpdated\" : " +
            "\"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/last")
    fun last30Days() = service.last30days()

}

@Service
class TwitterService(@Value("\${twitter.app-id}") val twitterAppId: String,
                     @Value("\${twitter.app-secret}") val twitterAppSecret: String,
                     @Value("\${twitter.token-id}") val twitterTokenId: String,
                     @Value("\${twitter.token-secret}") val twitterTokenSecret: String,
                     val repository: TwitterRepository) {
    private val logger = LoggerFactory.getLogger(TwitterService::class.java)!!

    private val confTwitter = ConfigurationBuilder().apply {
        logger.info("Connect to Twitter with appId=${twitterAppId.safeDisplaySecret()} " +
                "appSecret=${twitterAppSecret.safeDisplaySecret()} +" +
                "tokenId=${twitterTokenId.safeDisplaySecret()} " +
                "tokenSecret=${twitterTokenSecret.safeDisplaySecret()}")
        setOAuthConsumerKey(twitterAppId)
        setOAuthConsumerSecret(twitterAppSecret)
        setOAuthAccessToken(twitterTokenId)
        setOAuthAccessTokenSecret(twitterTokenSecret)
    }.build()
    private val twitter = TwitterFactory(confTwitter).instance
    private var favorites = 0
    private var followers = 0
    private var lastUpdated = LocalDateTime.now()!!

    fun currentLikes() = favorites
    fun currentFollowers() = followers
    fun lastUpdateDateTime() = lastUpdated
    fun last30days() = repository.findTop30ByOrderByDateDesc().map { it.followers } + currentFollowers()

    @Scheduled(fixedDelay = 700_000, initialDelay = 0)
    fun fetch() {
        favorites = twitter.users().showUser(781895128361345024).favouritesCount
        followers = twitter.users().showUser(781895128361345024).followersCount
        lastUpdated = LocalDateTime.now()
    }

    @Scheduled(cron = "0 59 23 * * ?")
    fun saveTwitterData() {
        repository.save(Twitter(date = LocalDate.now(), favorites = favorites, followers = followers))
    }
}

@Document
class Twitter(@Id val id: String = UUID.randomUUID().toString(),
              @Indexed val date: LocalDate,
              val favorites: Int,
              val followers: Int)

interface TwitterRepository : CrudRepository<Twitter, String> {
    fun findTop30ByOrderByDateDesc(): List<Twitter>
}