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
    fun currentMonthByDayCollect(): Graph {
        val collects = service.collectCurrentMonthByDay()
        return Graph(collects.keys.map { it.dayOfMonth.toString() }, collects.values)
    }

    @RequestMapping("/backers/month")
    fun currentMonthByDayBackers(): Graph {
        val backers = service.backersCurrentMonthByDay()
        return Graph(backers.keys.map { it.dayOfMonth.toString() }, backers.values)
    }

    @RequestMapping("/goal/month")
    fun currentMonthGoal() = service.goal()

    @RequestMapping("/reached/month")
    fun currentReachedGoal() = service.goalReached()
}

@Service
class KissKissBankBankService(val repository: KissKissBankBankRepository) {
    private val goal = 10000
    private var collect = 0
    private var backers = 0
    private var lastUpdated = LocalDateTime.now()!!

    fun goal() = goal
    fun goalReached() = totalCollectCurrentMonth() * 100 / goal()
    fun totalCollectCurrentMonth() =  currentMonthByDay().map { it.collect }.sum()
    fun collectCurrentMonthByDay() = currentMonthByDay().map { it.date to it.collect }.toMap()
    fun backersCurrentMonthByDay() = currentMonthByDay().map { it.date to it.backers }.toMap()
    fun currentBackers() = fetch(".bankers").replace(" ", "").toInt()
    fun currentCollect() = fetch(".collected_amount").replace("€", "").replace(" ", "").toInt()
    fun lastUpdateDateTime() = lastUpdated

    @Scheduled(cron = "0 59 23 * * ?")
    fun saveIndiegogoData() {
        repository.save(KissKissBankBank(date = LocalDate.now(), collect = collect, backers = backers))
    }

    @Scheduled(fixedDelay = 350_000, initialDelay = 0)
    final fun fetch() {
        collect = currentCollect()
        backers = currentBackers()
        lastUpdated = LocalDateTime.now()
    }

    private fun currentMonthByDay(): List<KissKissBankBank> {
        val firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfCurrentMonth = firstDayOfCurrentMonth.with(TemporalAdjusters.lastDayOfMonth())
        val currentMonthData = repository.findByDateBetween(firstDayOfCurrentMonth, lastDayOfCurrentMonth.plusDays(1))
        return (1..lastDayOfCurrentMonth.dayOfMonth).map {
            val currentDay = firstDayOfCurrentMonth.plusDays(it - 1L)
            currentMonthData.find { it.date == currentDay } ?: KissKissBankBank(date = currentDay, backers = 0, collect = 0)
        }
    }

    private fun fetch(css: String) = Jsoup.connect("https://www.kisskissbankbank.com/pup-le-mini-scanner-connecte-le-plus-rapide-du-monde?ref=selection")
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
