package apps;

import org.jooby.Jooby;
import org.jooby.Upload;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;

public class App1056 extends Jooby {

    {
        use(CatResource.class);
    }

    @Path("/api/inner/cat")
    public static class CatResource {

        @Path("/:name/pat")
        @POST
        public void pat(final String name) {
        }

        @Path("/:name/feed")
        @POST
        public void feed(final String name, final Upload food) {
        }
    }
}
