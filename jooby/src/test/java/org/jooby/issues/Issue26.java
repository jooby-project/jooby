package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Body;
import org.jooby.MediaType;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.typesafe.config.Config;

public class Issue26 extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    use(new Body.Formatter() {

      @Override
      public List<MediaType> types() {
        return ImmutableList.of(MediaType.html);
      }

      @Override
      public void format(final Object body, final Body.Writer writer) throws Exception {
        Config config = (Config) writer.locals().get("config");
        assertNotNull(config);
        writer.text(out -> CharStreams.copy(new StringReader(body.toString()), out));
        latch.countDown();
      }

      @Override
      public boolean canFormat(final Class<?> type) {
        return true;
      }
    });

    get("*", (req, rsp) -> req.set("config", req.require(Config.class)));

    get("/", req -> "OK");
  }

  @Test
  public void shouldHaveAccessToLocalVarFromBodyWriteContext() throws Exception {
    assertEquals("OK", execute(GET(uri("/")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
    latch.await();
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
