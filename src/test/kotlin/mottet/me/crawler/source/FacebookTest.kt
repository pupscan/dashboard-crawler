package mottet.me.crawler.source

import org.junit.Test

class FacebookTest {

    @Test
    fun displayLike() {
        val facebookController = FacebookController()
        facebookController.fetch()
        println(facebookController.favorite())
        println(facebookController.followers())
    }
}
