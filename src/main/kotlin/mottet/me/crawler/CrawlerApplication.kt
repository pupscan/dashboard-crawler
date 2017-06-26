package mottet.me.crawler

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.jsoup.Jsoup
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@EnableScheduling
@SpringBootApplication
class CrawlerApplication

fun main(args: Array<String>) {
    SpringApplication.run(CrawlerApplication::class.java, *args)
}


@RestController
@RequestMapping("/kkbb")
class KissKissBankBankController(val dateTime: DateTime) {
    var collect = fetchCollect()
    var backers = fetchBackers()
    var lastUpdated = lastUpdated()


    @RequestMapping("/collect")
    fun collect() = "{\"current\" : \"$collect\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : \"$backers\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @Scheduled(fixedDelay = 350_000)
    fun fetch() {
        collect = fetchCollect()
        backers = fetchBackers()
        lastUpdated = lastUpdated()
    }

    private fun lastUpdated() = dateTime.now()
    private fun fetchBackers() = fetch(".bankers")
    private fun fetchCollect() = fetch(".collected_amount").replace("€", "")
    private fun fetch(css : String) = Jsoup.connect("https://www.kisskissbankbank.com/pup-le-mini-scanner-connecte-le-plus-rapide-du-monde?ref=selection")
            .get()
            .select(css)
            .text()!!
}

@RestController
@RequestMapping("/indiegogo")
class IndiegogoController(val dateTime: DateTime) {
    var collect = fetchCollect()
    var backers = fetchBackers()
    var lastUpdated = lastUpdated()

    @RequestMapping("/collect")
    fun collect() = "{\"current\" : \"$collect\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : \"$backers\", \"lastUpdated\" :  \"$lastUpdated\"}"


    @Scheduled(fixedDelay = 350_000)
    fun fetch() {
        collect = fetchCollect()
        backers = fetchBackers()
        lastUpdated = lastUpdated()
    }

    private fun lastUpdated() = dateTime.now()
    private fun fetchBackers() = fetch("contributions_count")
    private fun fetchCollect() = fetch("collected_funds") + fetch("forever_funding_collected_funds")
    private fun fetch(fieldName : String) = RestTemplate().getForObject("https://api.indiegogo.com/1" +
            ".1/campaigns/1918821" +
            ".json?api_token=16e63457e7a24c06d39b40b52c0df273098cab82ccd3d4abaafd1a9c7a4edfe7", Response::class.java)
            .response[fieldName].toString().toInt()
}



@Service
class DateTime {
    val yyyy_MM_dd_hh_mm_ss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")!!

    fun now() = LocalDateTime.now().format(yyyy_MM_dd_hh_mm_ss)!!
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Response(val response: Map<String, Any>)
