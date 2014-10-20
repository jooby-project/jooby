package jooby;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import jooby.FilterFeature.HttpResponseValidator;
import jooby.Session.Store;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class DestroySessionByTimeoutFeature extends ServerFeature {

  private static final List<String> deletes = new ArrayList<>();

  private Store store = new Store() {

    @Override
    public Session get(final String id) throws Exception {
      return null;
    }

    @Override
    public void save(final Session session, final SaveReason reason) throws Exception {
    }

    @Override
    public void delete(final String id) throws Exception {
      System.out.println("deleting " + id);
      deletes.add(id);
    }

    @Override
    public String generateID(final long seed) {
      return "1234";
    }

  };

  {
    use(store)
        .timeout(1);

    get("/timeout", (req, res) -> {
      req.session();
      res.send("timeout");
    });

  }

  @Test
  public void destroyByTimeoutSession() throws Exception {
    String sessionId = "1234|anN8BeWjnfVFT4P/FGkN7YbYAPhfXvTCx7P9CBrPa/s";
    String cookieId = "jooby.sid=" + sessionId + ";Path=/;Secure;HttpOnly";

    assertEquals(
        "timeout",
        execute(
            GET(uri("timeout")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    Thread.sleep(1200L);

    assertEquals(
        "timeout",
        execute(
            GET(uri("timeout")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(sessionId, deletes.remove(0));
    assertEquals(0, deletes.size());
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
