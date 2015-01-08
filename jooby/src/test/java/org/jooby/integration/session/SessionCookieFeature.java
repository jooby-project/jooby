package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SessionCookieFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.secret",
        ConfigValueFactory.fromAnyRef("fixed")));

    use(new Session.MemoryStore()).cookie()
        .name("custom.sid")
        .path("/session")
        .comment("jooby cookie")
        .domain("localhost")
        .maxAge(60);

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
    });

  }

  @Test
  public void cookieConfig() throws Exception {
    long maxAge = System.currentTimeMillis() + 60 * 1000;
    // remove seconds to make sure test always work
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd-MMM-yyyy HH:mm");
    Instant instant = Instant.ofEpochMilli(maxAge);
    OffsetDateTime utc = instant.atOffset(ZoneOffset.UTC);
    execute(GET(uri("session")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      List<String> setCookie = Lists.newArrayList(Splitter.onPattern(";\\s*")
          .splitToList(rsp.getFirstHeader("Set-Cookie").getValue()));

      System.out.println(setCookie);
      assertTrue(setCookie.remove(0).startsWith("custom.sid"));
      assertTrue(setCookie.remove("path=/session"));
      assertTrue(setCookie.remove("HttpOnly"));
      assertTrue(setCookie.remove("Max-Age=60"));
      assertTrue(setCookie.remove("domain=localhost"));
      assertEquals(1, setCookie.size());
      assertTrue(setCookie.remove(0).startsWith(
          "Expires=" + formatter.format(utc).replace("GMT", "")));
    });

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
