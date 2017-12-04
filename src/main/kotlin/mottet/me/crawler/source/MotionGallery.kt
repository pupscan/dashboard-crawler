package mottet.me.crawler.source

import mottet.me.crawler.toReadableDate
import mottet.me.crawler.yenToDollar
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
@RequestMapping("/motion-gallery")
class MotionGalleryController(val service: MotionGalleryService) {

    @RequestMapping("/collect")
    fun collect() = "{\"current\" : ${service.currentCollect()}, \"lastUpdated\" :" +
            " \"${service.lastUpdateDateTime().toReadableDate()}\" }"

    @RequestMapping("/backers")
    fun backers() = "{\"current\" : ${service.currentBackers()}, \"lastUpdated\" : " +
            " \"${service.lastUpdateDateTime().toReadableDate()}\"}"

    @RequestMapping("/collect/month/total")
    fun totalCollectCurrentMonth() = service.collectCurrentMonth()
}

@Service
class MotionGalleryService(private val repository: MotionGalleryRepository) {
    private var collect = 4895777
    private var backers = 2618
    private var lastUpdated = LocalDateTime.now()!!

    fun currentBackers() = backers
    fun currentCollect() = collect
    fun lastUpdateDateTime() = lastUpdated
    fun collectCurrentMonth() = currentCollect() - totalCollectAtTheBeginningOfCurrentMonth()
    fun totalCollectAtTheBeginningOfCurrentMonth() : Int {
        val findByDate = repository
                .findByDate(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).minusDays(1))
        if (findByDate.isPresent) return findByDate.get().collect
        return 0
    }

    @Scheduled(cron = "0 59 23 * * ?") // Save to pacific time
    fun saveMotionGalleryData() {
        repository.save(MotionGallery(date = LocalDate.now().minusDays(1), collect = collect, backers = backers))
    }

    @Scheduled(fixedDelay = 350_000, initialDelay = 0)
    final fun fetch() {
        collect = fetch(".stats-table .money .number").yenToDollar().toInt()
        backers = fetch(".stats-table .collector .number")
        lastUpdated = LocalDateTime.now()
    }

    fun fetch(css: String) = Jsoup.connect("https://motion-gallery.net/projects/pupscan_2")
            .get()
            .select(css)
            .text().replace(",", "").toInt()

}


interface MotionGalleryRepository : CrudRepository<MotionGallery, String> {
    fun findByDate(date: LocalDate): Optional<MotionGallery>
}

@Document
class MotionGallery(@Id val id: String = UUID.randomUUID().toString(),
                    @Indexed val date: LocalDate,
                    val collect: Int,
                    val backers: Int)

