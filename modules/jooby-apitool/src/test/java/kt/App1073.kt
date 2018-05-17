package kt

import org.jooby.*
import java.time.ZonedDateTime

data class News(
        val title: String,
        val date: ZonedDateTime)

class App1073 : Kooby({
    get {
        News("Title", param<ZonedDateTime>("start"))
    }
})