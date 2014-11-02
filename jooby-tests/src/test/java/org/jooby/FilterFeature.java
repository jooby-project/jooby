package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FilterFeature extends ServerFeature {

  public interface HttpResponseValidator {
    public void validate(HttpResponse response) throws Exception;
  }

  {

    use((req, rsp, chain) -> {
      chain.next(req, rsp);
    });

    get("/no-next", (req, rsp, chain) -> {
    });

    get("/no-next", (req, rsp, chain) -> {
      throw new IllegalStateException("Should NOT execute ever");
    });

    get("/before", (req, rsp, chain) -> {
      rsp.header("before", "before");
      chain.next(req, rsp);
    });

    get("/before", (req, rsp) -> {
      rsp.send(rsp.header("before").stringValue());
    });

    get("/after", (req, rsp, chain) -> {
      chain.next(req, rsp);
      rsp.header("after", "after");
    });

    get("/after", (req, rsp) -> {
      rsp.send(rsp.header("after").toOptional(String.class).orElse("after-missing"));
    });

    get("/commit", (req, rsp, chain) -> {
      rsp.send("commit");
    });

    get("/commitx2", (req, rsp, chain) -> {
      rsp.send("commit1");
      chain.next(req, rsp);
    });

    get("/commitx2", (req, rsp) -> {
      rsp.send("ignored");
    });

    get("/redirect", (req, rsp, chain) -> {
      rsp.redirect("/commit");
      chain.next(req, rsp);
    });

    get("/redirect", (req, rsp) -> {
      rsp.send("ignored");
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
  public void redirectIsPossibleFromFilter() throws Exception {
    assertEquals("", execute(GET(uri("/redirect")), (response) -> {
      assertEquals(302, response.getStatusLine().getStatusCode());
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
    Executor executor = Executor.newInstance(HttpClientBuilder.create()
        .setRedirectStrategy(new RedirectStrategy() {

          @Override
          public boolean isRedirected(final HttpRequest request, final HttpResponse response,
              final HttpContext context) throws ProtocolException {
            return false;
          }

          @Override
          public HttpUriRequest getRedirect(final HttpRequest request, final HttpResponse response,
              final HttpContext context) throws ProtocolException {
            return null;
          }
        }).build());

    HttpResponse response = executor.execute(request).returnResponse();
    validator.validate(response);
    return EntityUtils.toString(response.getEntity());
  }

}
