package mottet.me.crawler.source

import org.junit.Test

class TwitterTest {

    @Test
    fun displayLike() {
        println(TwitterController().favorite())
        println(TwitterController().followers())
    }
}