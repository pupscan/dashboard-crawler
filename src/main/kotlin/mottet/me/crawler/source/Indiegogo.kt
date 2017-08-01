package mottet.me.crawler.source

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*


@RestController
@RequestMapping("/indiegogo")
class IndiegogoController(val service: IndiegogoService) {

    @RequestMapping("/collect")
    fun collect() = "{\"current\" : ${service.currentCollect()}, \"lastUpdated\" :" +
            " \"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : ${service.currentBackers()}, \"lastUpdated\" : " +
            " \"${service.lastUpdateDateTime().toReadableDate()}\"}"

    @RequestMapping("/collect/month")
    fun aggregateMonthByDayCollect(): Graph {
        val collects = service.collectAggregateMonthByDay()
        return Graph(collects.keys.map { it.dayOfMonth.toString() }, collects.values)
    }

    @RequestMapping("/collect/month/current")
    fun currentMonthByDayCollect(): Graph {
        val collects = service.collectCurrentMonthByDay()
        return Graph(collects.keys.map { it.dayOfMonth.toString() }, collects.values)
    }

    @RequestMapping("/backers/month")
    fun currentMonthByDayBackers(): Graph {
        val backers = service.backersAggregateMonthByDay()
        return Graph(backers.keys.map { it.dayOfMonth.toString() }, backers.values)
    }

    @RequestMapping("/collect/month/total")
    fun totalCollectCurrentMonth() = service.collectCurrentMonth()

    @RequestMapping("/goal/month")
    fun currentMonthGoal() = service.goal()

    @RequestMapping("/reached/month")
    fun currentReachedGoal() = service.goalReached()
}

@Service
class IndiegogoService(@Value("\${indiegogo.app-token}") val indiegogoToken: String,
                       val repository: IndiegogoRepository) {
    private val logger = LoggerFactory.getLogger(IndiegogoService::class.java)!!

//    private val difference = 376_256
    private val goal = 45000
    private var collect = 0
    private var backers = 0
    private var lastUpdated = LocalDateTime.now()!!

    init {
        logger.info("Connect to Indiegogo token=${indiegogoToken.safeDisplaySecret()}")
    }

    fun currentBackers() = backers
    fun currentCollect() = collect
    fun lastUpdateDateTime() = lastUpdated
    fun goal() = goal
    fun goalReached() = collectCurrentMonth() * 100 / goal()
    fun collectCurrentMonth() = currentCollect() - totalCollectAtTheBeginningOfCurrentMonth()
    fun collectCurrentMonthByDay() = currentMonthByDay().map { it.date to it.collect }.toMap()
    fun collectAggregateMonthByDay() = aggregateMonthByDay().map { it.date to it.collect }.toMap()
    fun backersAggregateMonthByDay() = aggregateMonthByDay().map { it.date to it.backers }.toMap()
    fun totalCollectAtTheBeginningOfCurrentMonth() = repository
            .findByDateBetween(LocalDate.ofYearDay(2017, 1), LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()))
            .map { it.collect }
            .sum()

    @Scheduled(cron = "0 59 23 * * ?")
    fun saveIndiegogoData() {
        repository.save(Indiegogo(date = LocalDate.now(), collect = collect, backers = backers))
    }

    @Scheduled(fixedDelay = 350_000, initialDelay = 0)
    final fun fetch() {
        collect = fetch("collected_funds") + fetch("forever_funding_collected_funds")
        backers = fetch("contributions_count")
        lastUpdated = LocalDateTime.now()
    }

    private fun currentMonthByDay(): List<Indiegogo> {
        val firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfCurrentMonth = firstDayOfCurrentMonth.with(TemporalAdjusters.lastDayOfMonth())
        val currentMonthData = repository
                .findByDateBetween(firstDayOfCurrentMonth, lastDayOfCurrentMonth.plusDays(1))
        val customMonthData = currentMonthData
                .mapIndexed { index, it ->
                    Indiegogo(it.id,
                            it.date,
                            it.collect - (currentMonthData.getOrNull(index - 1)?.collect ?: it.collect),
                            it.backers)
                } + Indiegogo(
                date = lastUpdateDateTime().toLocalDate(),
                collect = currentCollect() - (currentMonthData.lastOrNull()?.collect ?: 0),
                backers = currentBackers())
        return (1..lastDayOfCurrentMonth.dayOfMonth).map {
            val currentDay = firstDayOfCurrentMonth.plusDays(it - 1L)
            customMonthData.find { it.date == currentDay } ?: Indiegogo(date = currentDay, backers = 0, collect = 0)
        }
    }

    private fun aggregateMonthByDay(): List<Indiegogo> {
        val firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfCurrentMonth = firstDayOfCurrentMonth.with(TemporalAdjusters.lastDayOfMonth())
        val totalCollectAtTheBeginningOfCurrentMonth = totalCollectAtTheBeginningOfCurrentMonth()
        val currentMonthData = repository
                .findByDateBetween(firstDayOfCurrentMonth, lastDayOfCurrentMonth.plusDays(1))
                // TODO: fix next month
                .map { Indiegogo(it.id, it.date, it.collect - totalCollectAtTheBeginningOfCurrentMonth, it.backers) }.toMutableList()
        currentMonthData.add(Indiegogo(date = lastUpdateDateTime().toLocalDate(), collect = currentCollect() - totalCollectAtTheBeginningOfCurrentMonth, backers = currentBackers()))
        return (1L..currentMonthData.last().date.dayOfMonth).map {
            val currentDay = firstDayOfCurrentMonth.plusDays(it - 1)
            currentMonthData.find { it.date == currentDay } ?: Indiegogo(date = currentDay, backers = 0, collect = 0)
        }
    }

    private fun fetch(fieldName: String) = RestTemplate().getForObject("https://api.indiegogo.com/1" +
            ".1/campaigns/1918821" +
            ".json?api_token=$indiegogoToken", Response::class.java)
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

