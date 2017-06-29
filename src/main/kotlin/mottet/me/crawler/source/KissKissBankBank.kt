package mottet.me.crawler.source

import mottet.me.crawler.now
import mottet.me.crawler.toReadableNumber
import org.jsoup.Jsoup
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/kkbb")
class KissKissBankBankController {
    private var collect = 0
    private var backers = 0
    private lateinit var lastUpdated : String

    init {
        fetch()
    }

    @RequestMapping("/collect")
    fun collect() = "{\"current\" : \"${collect.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : \"${backers.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @Scheduled(fixedDelay = 350_000)
    final fun fetch() {
        collect = fetchCollect()
        backers = fetchBackers()
        lastUpdated = lastUpdated()
    }

    private fun lastUpdated() = now()
    private fun fetchBackers() = fetch(".bankers").replace(" ", "").toInt()
    private fun fetchCollect() = fetch(".collected_amount").replace("€", "").replace(" ", "").toInt()
    private fun fetch(css : String) = Jsoup.connect("https://www.kisskissbankbank.com/pup-le-mini-scanner-connecte-le-plus-rapide-du-monde?ref=selection")
            .get()
            .select(css)
            .text()!!
}