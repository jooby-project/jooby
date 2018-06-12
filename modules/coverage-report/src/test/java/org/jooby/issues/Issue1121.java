package org.jooby.issues;

import com.google.common.collect.ImmutableMap;
import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.util.Map;

public class Issue1121 extends ServerFeature {

  @Path("/1121")
  public static class MVC {
    @GET
    public String locals(@Local Map<String, Object> something) {
      return something.toString();
    }
  }

  {
    use("*", (req, rsp) -> {
      req.set("something", ImmutableMap.of("foo", "bar"));
    });

    use(MVC.class);
  }

  @Test
  public void localShouldFavorExistingMap() throws Exception {
    request().get("/1121")
        .expect("{foo=bar}");
  }
}
