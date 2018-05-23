package kt

import org.jooby.Kooby

data class Picture1096(val url: String)

data class Query1096(
        val name: String,
        val firstname: String?,
        val picture: Picture1096)

class App1096e : Kooby({
    path("/1096/kt") {
        get {
            val q = params(Query1096::class.java)
            q.toString()
        }
    }
})