package issues

import com.typesafe.config.Config
import org.jooby.Err
import org.jooby.Kooby

data class LoginRequest(val username: String)

class App987 : Kooby({
    /**
    Authenticates a user, generating an Authorization token.
    @param body contains the user's credentials.
    @return contains the apiToken
     */
    post("/api/login") { req ->
        val params: LoginRequest
        try {
            params = req.body(LoginRequest::class.java)
        } catch (e: Exception) {
            throw Err(401, "Could not parse request: ${e.message}")
        }
        params.username
    }

    get("/use/config") {
        val conf = require(Config::class.java)
        val useName = conf.getBoolean("name")
        val params: LoginRequest
        try {
            params = body(LoginRequest::class.java)
        } catch (e: Exception) {
            throw Err(401, "Could not parse request: ${e.message} ${useName}")
        }
        params.username
    }

})
