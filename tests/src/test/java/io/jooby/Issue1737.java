package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.pac4j.Pac4jModule;
import okhttp3.FormBody;
import okhttp3.Response;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;

import java.net.URLEncoder;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Issue1737 {

  private static final String WELCOME = "<!DOCTYPE html>\n"
      + "<html>\n"
      + "<head>\n"
      + "  <title>Welcome Page</title>\n"
      + "</head>\n"
      + "<body>\n"
      + "<h3>Welcome: {0}</h3>\n"
      + "<h4><a href=\"/logout\">Logout</a></h4>\n"
      + "</body>\n"
      + "</html>\n";

  @ServerTest
  public void pac4jShouldWorkWithSignedSession(ServerTestRunner runner) {
    runner.define(app -> {
      app.setSessionStore(
          SessionStore.signed("123456789", new Cookie("Test").setMaxAge(Duration.ofDays(7))));

      app.install(new Pac4jModule()
          .client(conf -> new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator()))
      );

      app.get("/",
          ctx -> ctx.setResponseType(MediaType.html).send(String.format(WELCOME, ctx.getUser())));

      app.get("/some/page",
          ctx -> ctx.setResponseType(MediaType.html).send(String.format(WELCOME, ctx.getUser())));
    }).dontFollowRedirects().ready(http -> {
      // Save URL
      String requestedPath = "http://localhost:9999/some/page";
      String cookie =
          "Test=ftnFEEumoZJTty9t2TI649TA285kfDDntIVFIaDLANw|pac4jCsrfToken=f780c42c-f750-4b35-bb3d-96660acec005&pac4jRequestedUrl=p4j%7E302%3A"
              + URLEncoder.encode(requestedPath, "UTF-8");
      http.header("Cookie", cookie);
      http.post("/callback?client_name=FormClient", new FormBody.Builder()
          .add("username", "test")
          .add("password", "test")
          .build(), rsp -> {
        String updatedCookie = cleanCookie(rsp);
        assertTrue(updatedCookie.contains("pac4jUserProfiles="), updatedCookie);
        assertEquals(requestedPath, rsp.header("Location"));
      });
    });
  }

  private String cleanCookie(Response response) {
    String value = response.headers("Set-Cookie").stream().filter(it -> it.startsWith("Test="))
        .findFirst()
        .get();
    int i = value.indexOf(";Path");
    return i > 0 ? value.substring(0, i) : value;
  }
}
