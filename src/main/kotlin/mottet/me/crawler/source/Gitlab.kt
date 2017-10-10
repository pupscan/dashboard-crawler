package mottet.me.crawler.source

import feign.RequestInterceptor
import org.springframework.cloud.netflix.feign.FeignClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@Configuration
class GitlabConfiguration {
    @Bean
    fun requestInterceptor() = RequestInterceptor { template -> template.header("PRIVATE-TOKEN", "pprGjpKAT69QiCh7BFvP") }
}

@FeignClient(name = "gitlab", url = "https://gitlab.com/api/v4/groups/pupscan",configuration = arrayOf(GitlabConfiguration::class))
interface GitlabClient {

    @RequestMapping("/milestones/388943/issues?per_page=100")
    fun mvpIssues(): List<Issue>
}

@RestController
@RequestMapping("/gitlab")
class GitlabController(val service: GitlabService) {

    @RequestMapping("/progression")
    fun progression() = "{\"current\" : ${service.currentProgression()} }"
}

@Service
class GitlabService(private val gitlabClient: GitlabClient) {
    private var progression = 0

    fun currentProgression() =  progression

    @Scheduled(fixedDelay = 350_000, initialDelay = 0)
    fun fetch() {
        progression = progression()
    }

    private fun progression(): Int {
        val issues = gitlabClient.mvpIssues()
        val total = issues.size
        val closed = issues.filter { it.state == "closed" }.size
        return closed * 100 / total
    }
}

data class Issue(val id: Long, val state: String)