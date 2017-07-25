package mottet.me.crawler

import mottet.me.crawler.source.IndiegogoService
import mottet.me.crawler.source.KissKissBankBankService
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
class CrowdfundingService(val indiegogoService: IndiegogoService, val kissBankBankService: KissKissBankBankService) {
    fun totalFund() = indiegogoService.currentCollect() + kissBankBankService.currentCollect().euroToDollar()
    fun totalBackers() = indiegogoService.currentBackers() + kissBankBankService.currentBackers()
    fun totalMonthFund() = indiegogoService.totalCollectCurrentMonth() + kissBankBankService.totalCollectCurrentMonth().euroToDollar()
}

