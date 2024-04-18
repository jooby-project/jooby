package examples;

import io.jooby.Jooby;
import io.jooby.OpenAPIModule;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

public class MvcDaggerApp extends Jooby {

    // simulate dagger app
    interface DaggerApp {
        Controller controller();
    }
    static class DaggerAppImpl implements DaggerApp {

        public Controller controller() {
            return new Controller();
        }

    }


    @Path("/")
    static class Controller {

        @GET("/welcome")
        public String sayHi() {
            return "hi";
        }
    }

    {
        install(new OpenAPIModule());
        DaggerApp daggerApp = new DaggerAppImpl();

        mvc(daggerApp.controller());
    }
}
