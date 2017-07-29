package mottet.me.crawler

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@EnableScheduling
@SpringBootApplication
class CrawlerApplication {
    @Bean
    fun corsConfigurer() =
            object : WebMvcConfigurerAdapter() {
                override fun addCorsMappings(registry: CorsRegistry) {
                    registry.addMapping("/**").allowedOrigins("*")
                }
            }
}

fun main(args: Array<String>) {
    SpringApplication.run(CrawlerApplication::class.java, *args)
}

/**
 * Util
 */
fun Int.toReadableNumber() = DecimalFormat("#,###", DecimalFormatSymbols(Locale.FRANCE)).format(this)!!

fun Int.euroToDollar() = this * 1.14785

fun String.safeDisplaySecret(): String {
    if (this.isBlank()) return ""
    return "X".repeat(this.length - 3) + this.substring(this.length - 3)
}

fun LocalDateTime.toReadableDate() = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!
