package mottet.me.crawler.source

import org.assertj.core.api.Assertions
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito

@Ignore
class KissKissBankBankTest {

    private val repository = Mockito.mock(KissKissBankBankRepository::class.java)

    @Test
    fun totalCollectCurrentMonthTest() {
        val service = KissKissBankBankService(repository)
        Assertions.assertThat(service.collectCurrentMonthByDay()).isNotEmpty
    }
}
