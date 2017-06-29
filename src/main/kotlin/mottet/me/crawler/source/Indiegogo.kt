package mottet.me.crawler.source

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import mottet.me.crawler.now
import mottet.me.crawler.toReadableNumber
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate


@RestController
@RequestMapping("/indiegogo")
class IndiegogoController {
    private var collect = 0
    private var backers = 0
    private lateinit var lastUpdated: String

    init {
        fetch()
    }

    @RequestMapping("/collect")
    fun collect() = "{\"current\" : \"${collect.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : \"${backers.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\"}"

    @Scheduled(fixedDelay = 350_000)
    final fun fetch() {
        collect = fetchCollect()
        backers = fetchBackers()
        lastUpdated = lastUpdated()
    }

    private fun lastUpdated() = now()
    private fun fetchBackers() = fetch("contributions_count")
    private fun fetchCollect() = fetch("collected_funds") + fetch("forever_funding_collected_funds")
    private fun fetch(fieldName: String) = RestTemplate().getForObject("https://api.indiegogo.com/1" +
            ".1/campaigns/1918821" +
            ".json?api_token=16e63457e7a24c06d39b40b52c0df273098cab82ccd3d4abaafd1a9c7a4edfe7", Response::class.java)
            .response[fieldName].toString().toInt()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Response(val response: Map<String, Any>)

