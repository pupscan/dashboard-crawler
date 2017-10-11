package mottet.me.crawler.source

import org.jsoup.Jsoup
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

@Ignore
class MotionGalleryTest {
    @Autowired lateinit var client: StripeClient
    @Autowired lateinit var service: StripeService

    @Test
    fun `shoud scan a tv show directory`() {
        val collect  = Jsoup.connect("https://motion-gallery.net/projects/pupscan")
                .get()
                .select(".stats-table .money .number")
                .text().replace(",", "")

        val backers  = Jsoup.connect("https://motion-gallery.net/projects/pupscan")
                .get()
                .select(".stats-table .collector .number")
                .text().replace(",", "")
    }


}