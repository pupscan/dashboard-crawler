package mottet.me.crawler.source


import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.netflix.feign.FeignClient
import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails
import org.springframework.security.oauth2.common.AuthenticationScheme
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

class FabricConfiguration(@Value("\${fabric.username}") val fabricUsername: String,
                          @Value("\${fabric.password}") val fabricPassword: String,
                          @Value("\${fabric.app-id}") val appId: String,
                          @Value("\${fabric.app-secret}") val appSecret: String) {

    @Bean
    fun requestInterceptor() = OAuth2FeignRequestInterceptor(DefaultOAuth2ClientContext(), ResourceOwnerPasswordResourceDetails().apply {
        authenticationScheme = AuthenticationScheme.form
        clientAuthenticationScheme = AuthenticationScheme.form
        accessTokenUri = "https://fabric.io/oauth/token"
        clientId = appId
        clientSecret = appSecret
        scope = listOf("organizations apps issues features account twitter_client_apps beta software answers")
        username = fabricUsername
        password = fabricPassword
    })
}

@RestController
@RequestMapping("/fabric")
class FabricController(val client: FabricClient) {
    @RequestMapping("/active")
    fun actibe() = "{\"current\" : ${client.activeNow().cardinality} }"

}


@FeignClient(name = "fabric", url = "https://fabric.io/api/v2", configuration = arrayOf(FabricConfiguration::class))
interface FabricClient {

    @RequestMapping("/organizations/58ec31309ea69613c500006a/apps/59ccedb2c491f81c71335572/growth_analytics/daily_active.json?end=1514160000")
    fun dailyActiveUser(@RequestParam("start") start: Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), @RequestParam("end") end: Long = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()): Any

    @RequestMapping("/organizations/58ec31309ea69613c500006a/apps/59ccedb2c491f81c71335572/growth_analytics/active_now.json")
    fun activeNow(): ActiveNow
}

data class ActiveNow(val cardinality: Int)
