package mottet.me.crawler.source

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import mottet.me.crawler.toReadableDate
import mottet.me.crawler.toReadableNumber
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.CrudRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*


@RestController
@RequestMapping("/indiegogo")
class IndiegogoController(val service: IndiegogoService) {

    @RequestMapping("/collect")
    fun collect() = "{\"current\" : \"${service.currentCollect().toReadableNumber()}\", \"lastUpdated\" :" +
            " \"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : \"${service.currentBackers().toReadableNumber()}\", \"lastUpdated\" : " +
            " \"${service.lastUpdateDateTime().toReadableDate()}\"}"

    @RequestMapping("/collect/month")
    fun currentMonthByDayCollect(): Graph {
        val collects = service.collectCurrentMonthByDay()
        return Graph(collects.keys.map { it.dayOfMonth.toString() }, collects.values)
    }

    @RequestMapping("/backers/month")
    fun currentMonthByDayBackers(): Graph {
        val backers = service.backersCurrentMonthByDay()
        return Graph(backers.keys.map { it.dayOfMonth.toString() }, backers.values)
    }

    @RequestMapping("/collect/month/total")
    fun totalCollectCurrentMonth() = service.totalCollectCurrentMonth()

    @RequestMapping("/goal/month")
    fun currentMonthGoal() = service.goal()

    @RequestMapping("/reached/month")
    fun currentReachedGoal() = service.goalReached()
}

@Service
class IndiegogoService(val repository: IndiegogoRepository) {
    private val difference = 376_256
    private val goal = 40000
    private var collect = 0
    private var backers = 0
    private var lastUpdated = LocalDateTime.now()!!

    fun goal() = goal
    fun goalReached() = totalCollectCurrentMonth() * 100 / goal()
    fun totalCollectCurrentMonth() = currentCollect() - difference
    fun collectCurrentMonthByDay() = currentMonthByDay().map { it.date to it.collect }.toMap()
    fun backersCurrentMonthByDay() = currentMonthByDay().map { it.date to it.backers }.toMap()
    fun currentBackers() = fetch("contributions_count")
    fun currentCollect() = fetch("collected_funds") + fetch("forever_funding_collected_funds")
    fun lastUpdateDateTime() = lastUpdated

    @Scheduled(cron = "0 59 23 * * ?")
    fun saveIndiegogoData() {
        repository.save(Indiegogo(date = LocalDate.now(), collect = collect, backers = backers))
    }

    @Scheduled(fixedDelay = 350_000, initialDelay = 0)
    final fun fetch() {
        collect = currentCollect()
        backers = currentBackers()
        lastUpdated = LocalDateTime.now()
    }

    private fun currentMonthByDay(): List<Indiegogo> {
        val firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfCurrentMonth = firstDayOfCurrentMonth.with(TemporalAdjusters.lastDayOfMonth())
        val currentMonthData = repository
                .findByDateBetween(firstDayOfCurrentMonth, lastDayOfCurrentMonth.plusDays(1))
                // TODO: fix next month
                .map { Indiegogo(it.id, it.date, it.collect - difference, it.backers) }.toMutableList()
        currentMonthData.add(Indiegogo(date = lastUpdateDateTime().toLocalDate(), collect = currentCollect() - difference, backers = currentBackers()))
        return (1L..lastDayOfCurrentMonth.dayOfMonth).map {
            val currentDay = firstDayOfCurrentMonth.plusDays(it - 1)
            currentMonthData.find { it.date == currentDay } ?: Indiegogo(date = currentDay, backers = 0, collect = 0)
        }
    }

    private fun fetch(fieldName: String) = RestTemplate().getForObject("https://api.indiegogo.com/1" +
            ".1/campaigns/1918821" +
            ".json?api_token=16e63457e7a24c06d39b40b52c0df273098cab82ccd3d4abaafd1a9c7a4edfe7", Response::class.java)
            .response[fieldName].toString().toInt()
}

interface IndiegogoRepository : CrudRepository<Indiegogo, String> {
    fun findByDateBetween(from: LocalDate, to: LocalDate): List<Indiegogo>
}

data class Graph(val labels: Collection<String>, val data: Collection<Int>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Response(val response: Map<String, Any>)

@Document
class Indiegogo(@Id val id: String = UUID.randomUUID().toString(),
                @Indexed val date: LocalDate,
                val collect: Int,
                val backers: Int)

