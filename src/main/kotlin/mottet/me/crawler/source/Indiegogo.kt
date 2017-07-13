package mottet.me.crawler.source

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import mottet.me.crawler.now
import mottet.me.crawler.toReadableNumber
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.CrudRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*


@RestController
@RequestMapping("/indiegogo")
class IndiegogoController(val repository: IndiegogoRepository) {
    private var collect = 0
    private var backers = 0
    private var lastUpdated = now()

    @RequestMapping("/collect")
    fun collect() = "{\"current\" : \"${collect.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : \"${backers.toReadableNumber()}\", \"lastUpdated\" :  \"$lastUpdated\"}"

    @RequestMapping("/collect/month")
    fun currentMonthByDayCollect(): Graph {
        val firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfCurrentMonth = firstDayOfCurrentMonth.with(TemporalAdjusters.lastDayOfMonth())
        val labelsAndData = repository.findByDateBetween(firstDayOfCurrentMonth, lastDayOfCurrentMonth)
                .map { it.date to it.collect }
                .toMap()
        return Graph(labelsAndData.keys.map { it.dayOfMonth.toString() }, labelsAndData.values)
    }

    @RequestMapping("/backers/month")
    fun currentMonthByDayBackers(): Graph {
        val firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfCurrentMonth = firstDayOfCurrentMonth.with(TemporalAdjusters.lastDayOfMonth())
        val labelsAndData = repository.findByDateBetween(firstDayOfCurrentMonth, lastDayOfCurrentMonth)
                .map { it.date to it.backers }
                .toMap()
        return Graph(labelsAndData.keys.map { it.dayOfMonth.toString() }, labelsAndData.values)
    }

    @Scheduled(fixedDelay = 350_000, initialDelay = 0)
    final fun fetch() {
        collect = fetchCollect()
        backers = fetchBackers()
        lastUpdated = now()
    }

    @Scheduled(cron = "0 59 23 * * ?")
    fun saveMetric() {
        repository.save(Indiegogo(date = LocalDate.now(), collect = collect, backers = backers))
    }

    private fun fetchBackers() = fetch("contributions_count")
    private fun fetchCollect() = fetch("collected_funds") + fetch("forever_funding_collected_funds")
    private fun fetch(fieldName: String) = RestTemplate().getForObject("https://api.indiegogo.com/1" +
            ".1/campaigns/1918821" +
            ".json?api_token=16e63457e7a24c06d39b40b52c0df273098cab82ccd3d4abaafd1a9c7a4edfe7", Response::class.java)
            .response[fieldName].toString().toInt()
}

data class Graph(val labels: Collection<String>, val data: Collection<Int>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Response(val response: Map<String, Any>)

@Document
class Indiegogo(@Id val id: String = UUID.randomUUID().toString(),
                @Indexed val date: LocalDate,
                val collect: Int,
                val backers: Int)

interface IndiegogoRepository : CrudRepository<Indiegogo, String> {
    fun findByDateBetween(from: LocalDate, to: LocalDate): List<Indiegogo>
}