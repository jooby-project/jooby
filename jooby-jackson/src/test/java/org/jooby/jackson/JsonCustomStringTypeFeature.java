package org.jooby.jackson;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.TypeLiteral;

@SuppressWarnings("unchecked")
public class JsonCustomStringTypeFeature extends ServerFeature {

  {

    use(new Json().types("application/vnd.github.v3+json"));

    get("/members",
        req -> Lists.newArrayList(ImmutableMap.<String, Object> of("id", 1, "name", "pablo")));

    post("/members", req -> {
      List<Map<String, Object>> members = req.body(new TypeLiteral<List<Map<String, Object>>>() {
      });
      return members;
    });
  }

  @Test
  public void get() throws URISyntaxException, Exception {
    HttpResponse rsp = Request.Get(uri("members").build()).execute().returnResponse();
    assertEquals("application/vnd.github.v3+json; charset=UTF-8", rsp
        .getFirstHeader("Content-Type").getValue());
  }

  @Test
  public void post() throws URISyntaxException, Exception {
    assertEquals(
        "[{\"id\":1,\"name\":\"vilma\"}]",
        Request
            .Post(uri("members").build())
            .bodyString("[{\"id\":1,\"name\":\"vilma\"}]",
                ContentType.parse("application/vnd.github.v3+json")).execute()
            .returnContent().asString());
  }

  @Test
  public void err415() throws URISyntaxException, Exception {
    assertEquals(415, Request.Post(uri("members").build())
        .bodyString("[{\"id\":1,\"name\":\"vilma\"}]", ContentType.APPLICATION_JSON).execute()
        .returnResponse().getStatusLine().getStatusCode());
  }

}
