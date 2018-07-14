package issues;

import org.jooby.Route;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue398 extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/src")
    public Object src(final Route route) {
      return route.source();
    }
  }

  {
    get("/src", req -> {
      return req.route().source();
    });

    path("/g", () -> {
      get("/src", req -> {
        return req.route().source();
      });
    });
    get("/src1", "src2", req -> {
      return req.route().source();
    });

    use(Resource.class);
  }

  @Test
  public void directSrc() throws Exception {
    request()
        .get("/src")
        .expect("issues.Issue398:20");
  }

  @Test
  public void groupSrc() throws Exception {
    request()
        .get("/g/src")
        .expect("issues.Issue398:25");
  }

  @Test
  public void collection() throws Exception {
    request()
        .get("/src1")
        .expect("issues.Issue398:29");
  }

  @Test
  public void resource() throws Exception {
    request()
        .get("/r/src")
        .expect("issues.Issue398$Resource:14");
  }
}
