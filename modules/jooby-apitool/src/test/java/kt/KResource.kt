package kt

import org.jooby.mvc.GET
import org.jooby.mvc.Path

data class KR1(val name: String)

/**
 * KR API.
 */
@Path("/kr")
class KResource {
    /**
     * List KR.
     *
     * @param name KR name.
     */
    @GET
    fun list(name: String): List<KR1> {
        val result = listOf(KR1(name))
        doWith(result)
        return result
    }

    private fun doWith(result: List<KR1>) {
        println(result)
    }
}

