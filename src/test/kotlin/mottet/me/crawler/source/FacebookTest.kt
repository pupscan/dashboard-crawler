package mottet.me.crawler.source

import facebook4j.FacebookFactory
import org.junit.Test

class FacebookTest {

    val facebook = FacebookFactory().instance.apply {
        setOAuthAppId("896361467138797", "8f2ce15613d4d8d8d70212de6a2b944b")
        oAuthAccessToken = oAuthAppAccessToken
    }


    @Test
    fun displayLike() {
    }
}
