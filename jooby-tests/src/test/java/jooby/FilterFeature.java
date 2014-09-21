package jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FilterFeature extends ServerFeature {

  public interface HttpResponseValidator {
    public void validate(HttpResponse response);
  }

  {

    use((req, resp, chain) -> {
      chain.next(req, resp);
    });

    get("/no-next", (req, resp, chain) -> {
    });

    get("/no-next", (req, resp, chain) -> {
      throw new IllegalStateException("Should NOT execute ever");
    });

    get("/before", (req, resp, chain) -> {
      resp.header("before").setString("before");
      chain.next(req, resp);
    });

    get("/before", (req, resp) -> {
      resp.send(resp.header("before").getString());
    });

    get("/after", (req, resp, chain) -> {
      chain.next(req, resp);
      resp.header("after").setString("after");
    });

    get("/after", (req, resp) -> {
      resp.send(resp.header("after").getOptional(String.class).orElse("after-missing"));
    });

    get("/commit", (req, resp, chain) -> {
      resp.send("commit");
    });

    get("/commitx2", (req, resp, chain) -> {
      resp.send("commit1");
      chain.next(req, resp);
    });

    get("/commitx2", (req, resp) -> {
      resp.send("ignored");
    });

  }

  @Test
  public void nextFilterShouldNeverBeExecutedWhenChainNextIsMissing() throws Exception {
    assertEquals("", execute(GET(uri("/no-next")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void globalFilterShouldNOTAffect404Response() throws Exception {
    execute(GET(uri("/404")), (response) -> {
      assertEquals(404, response.getStatusLine().getStatusCode());
    });
  }

  @Test
  public void beforeFilterShouldBeExecuted() throws Exception {
    assertEquals("before", execute(GET(uri("/before")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertEquals("before", response.getFirstHeader("before").getValue());
    }));
  }

  @Test
  public void headerAfterResponseCommittedAreIgnored() throws Exception {
    assertEquals("after-missing", execute(GET(uri("/after")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertNull("after", response.getFirstHeader("after"));
    }));
  }

  @Test
  public void commitIsPossibleFromFilter() throws Exception {
    assertEquals("commit", execute(GET(uri("/commit")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void secondCommitIsIgnored() throws Exception {
    assertEquals("commit1", execute(GET(uri("/commitx2")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
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
