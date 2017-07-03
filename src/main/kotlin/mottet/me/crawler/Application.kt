package mottet.me.crawler

import mottet.me.crawler.source.Facebook
import mottet.me.crawler.source.FacebookRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@EnableScheduling
@EnableRedisRepositories
@SpringBootApplication
class CrawlerApplication {
    @Bean
    fun corsConfigurer() =
            object : WebMvcConfigurerAdapter() {
                override fun addCorsMappings(registry: CorsRegistry) {
                    registry.addMapping("/**").allowedOrigins("http://localhost:8081")
                }
            }

    @Bean
    fun connectionFactory() = JedisConnectionFactory()

    @Bean
    fun redisTemplate() = RedisTemplate<ByteArray, ByteArray>().apply {
        connectionFactory = connectionFactory()
    }

}

fun main(args: Array<String>) {
    SpringApplication.run(CrawlerApplication::class.java, *args)
}

@Component
class Simulatron(val repository: FacebookRepository) : CommandLineRunner {

    override fun run(vararg p0: String?) {
        repository.deleteAll()
        repository.save(Facebook(date = LocalDate.now().toEpochDay(), followers = 1100, favorites = 99))
        repository.save(Facebook(date = LocalDate.now().minusDays(1).toEpochDay(), followers = 1000, favorites = 59))
        repository.save(Facebook(date = LocalDate.now().minusDays(2).toEpochDay(), followers = 600, favorites = 49))
        repository.save(Facebook(date = LocalDate.now().minusDays(3).toEpochDay(), followers = 500, favorites = 19))


    }

}


/**
 * Util
 */
fun now() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!


