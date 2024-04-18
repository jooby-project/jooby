package examples;

import io.jooby.Jooby;
import io.jooby.OpenAPIModule;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

public class MvcRequireApp extends Jooby {

    @Path("/")
    static class Controller {

        @GET("/welcome")
        public String sayHi() {
            return "hi";
        }
    }

    {
        install(new OpenAPIModule());

        mvc(require(Controller.class));
    }
}
