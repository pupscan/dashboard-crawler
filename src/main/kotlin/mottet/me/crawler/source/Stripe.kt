package mottet.me.crawler.source

import feign.RequestInterceptor
import mottet.me.crawler.toReadableDate
import org.springframework.cloud.netflix.feign.FeignClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
@RequestMapping("/stripe")
class StripeController(val service: StripeService) {

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
class StripeService(private val stripeClient: StripeClient,
                    private val repository: StripeRepository) {
    private var collect = 0
    private var backers = 0
    private var lastUpdated = LocalDateTime.now()!!


    fun currentBackers() = backers
    fun currentCollect() = collect
    fun lastUpdateDateTime() = lastUpdated
    fun collectCurrentMonth() = currentCollect() - totalCollectAtTheBeginningOfCurrentMonth()
    fun totalCollectAtTheBeginningOfCurrentMonth(): Int {
        val findByDate = repository
                .findByDate(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).minusDays(1))
        if (findByDate.isPresent) return findByDate.get().collect
        return 0
    }

    @Scheduled(cron = "0 59 8 * * ?") // Save to pacific time
    fun saveStripeData() {
        repository.save(Stripe(date = LocalDate.now().minusDays(1), collect = collect, backers = backers))
    }

    @Scheduled(fixedDelay = 350_000, initialDelay = 0)
    final fun fetch() {
        val payouts = stripeClient.payout().data
        collect = total(payouts)
        backers = payouts.size
        lastUpdated = LocalDateTime.now()
    }

    private fun total(payouts: List<Payout>): Int {
        val totalCents = payouts
                .filter { it.status == "paid" }
                .map { it.amount }
                .toList()
                .sum()
        return totalCents / 100
    }

}

interface StripeRepository : CrudRepository<Stripe, String> {
    fun findByDate(date: LocalDate): Optional<Stripe>
}

@Document
class Stripe(@Id val id: String = UUID.randomUUID().toString(),
             @Indexed val date: LocalDate,
             val collect: Int,
             val backers: Int)

@Configuration
class StripeClientConfiguration {
    @Bean
    fun requestInterceptor() = RequestInterceptor { template -> template.header("Authorization", "Bearer rk_live_k6cxHT1Qw2f1pHSAJdGUjKyI") }
}

@FeignClient(name = "stripe", url = "https://api.stripe.com/v1", configuration = arrayOf(StripeClientConfiguration::class))
interface StripeClient {

    @RequestMapping("/payouts?limit=100")
    fun payout(): StripeResult
}

data class StripeResult(val data: List<Payout>)
data class Payout(val amount: Int, val currency: String, val status: String)