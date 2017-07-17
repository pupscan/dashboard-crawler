package mottet.me.crawler

import mottet.me.crawler.source.IndiegogoRepository
import mottet.me.crawler.source.KissKissBankBankRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@RestController
@RequestMapping("/crowdfunding")
class CrowdfundingController(val service: CrowdfundingService) {

    @RequestMapping("/collect/month")
    fun collect() = "{\"current\" : \"${service.totalCurrentMonth()}\" }"

}

@Service
class CrowdfundingService(val indiegogoRepository: IndiegogoRepository, val kissBankBankRepository: KissKissBankBankRepository) {

    fun totalCurrentMonth() = 0L

}

