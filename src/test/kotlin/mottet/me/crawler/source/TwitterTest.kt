package mottet.me.crawler.source

import org.junit.Test

class TwitterTest {

    @Test
    fun displayLike() {
        val twitterController = TwitterController()
        twitterController.fetch()
        println(twitterController.favorite())
        println(twitterController.followers())
    }
}