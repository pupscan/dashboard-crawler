package mottet.me.crawler.source

import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@Ignore
class GitlabTest {
    @Autowired lateinit var gitlabService: GitlabService


    @Test
    fun `shoud scan a tv show directory`() {
//        assertThat(gitlabService.progression()).isEqualTo(32)
    }


}
