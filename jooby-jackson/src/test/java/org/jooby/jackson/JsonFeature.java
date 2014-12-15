package org.jooby.jackson;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
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
    public Object post(final List<Map<String, Object>> body) {
      return body;
    }
  }

  {
    use(new Json());

    get("/members", req ->
      Lists.newArrayList(ImmutableMap.<String, Object> of("id", 1, "name", "pablo"))
    );

    post("/members", req -> {
      List<Map<String, Object>> members = req.body(new TypeLiteral<List<Map<String, Object>>>() {
      });
      return members;
    });

    use(Mvc.class);

  }

  @Test
  public void get() throws URISyntaxException, Exception {
    assertEquals("[{\"id\":1,\"name\":\"pablo\"}]", Request.Get(uri("members").build()).execute()
        .returnContent().asString());
  }

  @Test
  public void post() throws URISyntaxException, Exception {
    assertEquals("[{\"id\":1,\"name\":\"vilma\"}]", Request.Post(uri("members").build())
        .bodyString("[{\"id\":1,\"name\":\"vilma\"}]", ContentType.APPLICATION_JSON).execute()
        .returnContent().asString());

    assertEquals("[{\"id\":1,\"name\":\"vilma\"}]", Request.Post(uri("r", "members").build())
        .bodyString("[{\"id\":1,\"name\":\"vilma\"}]", ContentType.APPLICATION_JSON).execute()
        .returnContent().asString());
  }

}
