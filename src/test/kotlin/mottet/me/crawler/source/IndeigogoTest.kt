package mottet.me.crawler.source

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.Mockito

class IndeigogoTest {

    private val repository = Mockito.mock(IndiegogoRepository::class.java)

    @Test
    fun totalCollectCurrentMonthTest() {
        val service = IndiegogoService("", repository)
        Assertions.assertThat(service.collectCurrentMonthByDay()).isNotEmpty
    }
}
