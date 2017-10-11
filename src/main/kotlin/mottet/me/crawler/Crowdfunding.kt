package mottet.me.crawler

import mottet.me.crawler.source.IndiegogoService
import mottet.me.crawler.source.KissKissBankBankService
import mottet.me.crawler.source.MotionGalleryService
import mottet.me.crawler.source.StripeService
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/crowdfunding")
class CrowdfundingController(val service: CrowdfundingService) {

    @RequestMapping("/collect")
    fun totalCollect() = "{\"current\" : \"${service.totalFund()}\" }"

    @RequestMapping("/collect/month")
    fun totalCollectMonth() = "{\"current\" : \"${service.totalMonthFund()}\" }"

    @RequestMapping("/backers")
    fun totalBackers() = "{\"current\" : \"${service.totalBackers()}\" }"

}

@Service
class CrowdfundingService(private val indiegogoService: IndiegogoService,
                          private val kissBankBankService: KissKissBankBankService,
                          private val stripeService: StripeService,
                          private val motionGalleryService: MotionGalleryService) {
    fun totalFund() = indiegogoService.currentCollect() +
            kissBankBankService.currentCollect().euroToDollar() +
            stripeService.currentCollect().euroToDollar() +
            motionGalleryService.currentCollect()
    fun totalBackers() = indiegogoService.currentBackers() +
            kissBankBankService.currentBackers() +
            stripeService.currentBackers() +
            motionGalleryService.currentBackers()
    fun totalMonthFund() = indiegogoService.collectCurrentMonth() +
            kissBankBankService.totalCollectCurrentMonth().euroToDollar() +
            stripeService.collectCurrentMonth().euroToDollar() +
            motionGalleryService.collectCurrentMonth()
}

