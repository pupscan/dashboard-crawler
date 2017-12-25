package mottet.me.crawler.source

import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@Ignore
@SpringBootTest
class FabricTest {
    @Autowired lateinit var fabric: FabricClient


    @Test
    fun `shoud scan a tv show directory`() {
        val test = fabric.activeNow()

        assertThat(test.cardinality).isEqualTo(10)
    }


}
