package org.jooby.jackson;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.jooby.json.Jackson;
import org.jooby.mvc.Body;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.TypeLiteral;

@SuppressWarnings("unchecked")
public class JsonFeature extends ServerFeature {

  @Path("/r")
  public static class Mvc {

    @Path("/members")
    @POST
    public Object post(final @Body List<Map<String, Object>> body) {
      return body;
    }
  }

  {
    use(new Jackson());

    get("/members", req ->
        Lists.newArrayList(ImmutableMap.<String, Object> of("id", 1, "name", "pablo")));

    post("/members", req -> {
      List<Map<String, Object>> members = req.body().to(
          new TypeLiteral<List<Map<String, Object>>>() {
          });
      return members;
    });

    use(Mvc.class);

  }

  @Test
  public void get() throws URISyntaxException, Exception {
    request()
        .get("/members")
        .expect("[{\"id\":1,\"name\":\"pablo\"}]")
        .header("Content-Type", "application/json;charset=UTF-8");
  }

  @Test
  public void post() throws URISyntaxException, Exception {
    request()
        .post("/members")
        .body("[{\"id\":1,\"name\":\"vilma\"}]", "application/json")
        .expect("[{\"id\":1,\"name\":\"vilma\"}]")
        .header("Content-Type", "application/json;charset=UTF-8");

    request()
        .post("/r/members")
        .body("[{\"id\":1,\"name\":\"vilma\"}]", "application/json")
        .expect("[{\"id\":1,\"name\":\"vilma\"}]")
        .header("Content-Type", "application/json;charset=UTF-8");

  }

}
