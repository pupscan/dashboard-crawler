package mottet.me.crawler.source

import mottet.me.crawler.toReadableDate
import mottet.me.crawler.toReadableNumber
import org.jsoup.Jsoup
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.CrudRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*

@RestController
@RequestMapping("/kkbb")
class KissKissBankBankController(val service: KissKissBankBankService) {

    @RequestMapping("/collect")
    fun collect() = "{\"current\" : \"${service.currentCollect().toReadableNumber()}\", \"lastUpdated\" : " +
            " \"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : \"${service.currentBackers().toReadableNumber()}\", \"lastUpdated\" : " +
            " \"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/collect/month")
    fun aggregateMonthByDayCollect(): Graph {
        val collects = service.collectAggregateMonthBydDay()
        return Graph(collects.keys.map { it.dayOfMonth.toString() }, collects.values)
    }

    @RequestMapping("/collect/month/current")
    fun currentMonthByDayCollect(): Graph {
        val collects = service.collectCurrentMonthByDay()
        return Graph(collects.keys.map { it.dayOfMonth.toString() }, collects.values)
    }

    @RequestMapping("/backers/month")
    fun aggregateMonthByDayBackers(): Graph {
        val backers = service.backersAggregateMonthByDay()
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
class KissKissBankBankService(val repository: KissKissBankBankRepository) {
    private val difference = 7_599
    private val goal = 10000
    private var collect = 0
    private var backers = 0
    private var lastUpdated = LocalDateTime.now()!!

    fun currentBackers() = backers
    fun currentCollect() = collect
    fun lastUpdateDateTime() = lastUpdated
    fun goal() = goal
    fun goalReached() = totalCollectCurrentMonth() * 100 / goal()
    fun totalCollectCurrentMonth() = currentCollect() - difference
    fun collectCurrentMonthByDay() = currentMonthByDay().map { it.date to it.collect }.toMap()
    fun collectAggregateMonthBydDay() = aggregateMonthByDay().map { it.date to it.collect }.toMap()

    fun backersAggregateMonthByDay() = aggregateMonthByDay().map { it.date to it.backers }.toMap()

    @Scheduled(cron = "0 59 23 * * ?")
    fun saveIndiegogoData() {
        repository.save(KissKissBankBank(date = LocalDate.now(), collect = collect, backers = backers))
    }

    @Scheduled(fixedDelay = 350_000, initialDelay = 0)
    final fun fetch() {
        collect = fetch(".bankers").replace(" ", "").toInt()
        backers = fetch(".collected_amount").replace("€", "").replace(" ", "").toInt()
        lastUpdated = LocalDateTime.now()
    }

    private fun currentMonthByDay(): List<KissKissBankBank> {
        val firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfCurrentMonth = firstDayOfCurrentMonth.with(TemporalAdjusters.lastDayOfMonth())
        val currentMonthData = repository
                .findByDateBetween(firstDayOfCurrentMonth, lastDayOfCurrentMonth.plusDays(1))
        val customMonthData = currentMonthData
                .mapIndexed { index, it ->
                    KissKissBankBank(it.id,
                            it.date,
                            it.collect - (currentMonthData.getOrNull(index - 1)?.collect ?: it.collect),
                            it.backers)
                } + KissKissBankBank(
                date = lastUpdateDateTime().toLocalDate(),
                collect = currentCollect() - currentMonthData.last().collect,
                backers = currentBackers())
        return (1..lastDayOfCurrentMonth.dayOfMonth).map {
            val currentDay = firstDayOfCurrentMonth.plusDays(it - 1L)
            customMonthData.find { it.date == currentDay } ?: KissKissBankBank(date = currentDay, backers = 0, collect = 0)
        }
    }


    private fun aggregateMonthByDay(): List<KissKissBankBank> {
        val firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfCurrentMonth = firstDayOfCurrentMonth.with(TemporalAdjusters.lastDayOfMonth())
        val currentMonthData = repository
                .findByDateBetween(firstDayOfCurrentMonth, lastDayOfCurrentMonth.plusDays(1))
                // TODO: fix next month
                .map { KissKissBankBank(it.id, it.date, it.collect - difference, it.backers) }.toMutableList()
        currentMonthData.add(KissKissBankBank(date = lastUpdateDateTime().toLocalDate(), collect = currentCollect() - difference, backers = currentBackers()))
        return (1L..currentMonthData.last().date.dayOfMonth).map {
            val currentDay = firstDayOfCurrentMonth.plusDays(it - 1L)
            currentMonthData.find { it.date == currentDay } ?: KissKissBankBank(date = currentDay, backers = 0, collect = 0)
        }
    }

    private fun fetch(css: String) = Jsoup.connect("https://www.kisskissbankbank.com/fr/projects/pup-le-mini-scanner-connecte-le-plus-rapide-du-monde/wall")
            .get()
            .select(css)
            .text()!!
}


interface KissKissBankBankRepository : CrudRepository<KissKissBankBank, String> {
    fun findByDateBetween(from: LocalDate, to: LocalDate?): List<KissKissBankBank>
}

@Document
class KissKissBankBank(@Id val id: String = UUID.randomUUID().toString(),
                       @Indexed val date: LocalDate,
                       val collect: Int,
                       val backers: Int)
