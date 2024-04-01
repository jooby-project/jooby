package issues.i3397;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/")
public class Controller3397 {

    @GET("/welcome")
    public String sayHi() {
        return "hi";
    }

}
