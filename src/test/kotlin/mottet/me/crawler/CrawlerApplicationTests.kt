package mottet.me.crawler

import org.jsoup.Jsoup
import org.junit.Test
import org.springframework.web.client.RestTemplate

//@RunWith(SpringRunner::class)
//@SpringBootTest
class CrawlerApplicationTests {

	@Test
	fun contextLoads() {
		val select = Jsoup.connect("https://www.indiegogo.com/projects/pup-your-connected-pocket-scanner-home#/")
                .get()
                .select("script").first()

		println(select.toString())
	}

	@Test
	fun indiegogo() {
		println(fetchCollect())
	}

	private fun fetchCollect() = fetch("collected_funds") + fetch("forever_funding_collected_funds")
	private fun fetch(fieldName : String) = RestTemplate().getForObject("https://api.indiegogo.com/1" +
			".1/campaigns/1918821" +
			".json?api_token=16e63457e7a24c06d39b40b52c0df273098cab82ccd3d4abaafd1a9c7a4edfe7", Response::class.java)
			.response[fieldName].toString().toInt()


}
