package mottet.me.crawler

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@EnableScheduling
@SpringBootApplication
class CrawlerApplication {
    @Bean
    fun corsConfigurer() =
            object : WebMvcConfigurerAdapter() {
                override fun addCorsMappings(registry: CorsRegistry) {
                    registry.addMapping("/**").allowedOrigins("http://localhost:8081")
                }
            }
}

fun main(args: Array<String>) {
    SpringApplication.run(CrawlerApplication::class.java, *args)
}

/**
 * Util
 */
fun now() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!


