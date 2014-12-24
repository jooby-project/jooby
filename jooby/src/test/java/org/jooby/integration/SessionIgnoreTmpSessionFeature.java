package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class SessionIgnoreTmpSessionFeature extends ServerFeature {

  private static final AtomicInteger track = new AtomicInteger(0);

  {
    use(new Session.Store() {
      @Override
      public void save(final Session session, final SaveReason reason) {
        track.incrementAndGet();
      }

      @Override
      public Session get(final Session.Builder builder) {
        return null;
      }

      @Override
      public void delete(final String id) {
        track.incrementAndGet();
      }

      @Override
      public String generateID(final long seed) {
        return "123";
      }
    });

    get("/tmp", (req, rsp) -> {
      Session session = req.session();
      session.set("x", "c");
      session.destroy();
      rsp.send("tmp");
    });
  }

  @Test
  public void tmpSessionMustNotBeSaved() throws Exception {
    execute(GET(uri("tmp")), response -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      List<String> setCookie = Lists.newArrayList(Splitter.onPattern(";\\s*").splitToList(
          response.getFirstHeader("Set-Cookie").getValue()));
      assertTrue(setCookie.remove("jooby.sid=123"));
      assertTrue(setCookie.remove("path=/"));
      assertTrue(setCookie.remove("secure"));
      assertTrue(setCookie.remove("HttpOnly"));
      assertTrue(setCookie.remove("Max-Age=0"));
      assertEquals(1, setCookie.size());
      assertTrue(setCookie.remove(0).startsWith("Expires=Thu, 01-Jan-1970 00:00:00"));
    });

    Thread.sleep(2000L);

    assertEquals(0, track.get());
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
