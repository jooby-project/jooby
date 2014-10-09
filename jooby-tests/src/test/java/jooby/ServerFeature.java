package jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;

@RunWith(JoobyRunner.class)
public abstract class ServerFeature extends Jooby {

  public interface HttpCall {

    void call() throws Exception;

  }

  public static class HttpAssert {

    private HttpResponse response;

    public HttpAssert(final HttpResponse response) {
      this.response = response;
    }

    public HttpAssert status(final Response.Status status) throws Exception {
      doAssert(() -> assertEquals(status,
          Response.Status.valueOf(response.getStatusLine().getStatusCode())));
      return this;
    }

    public HttpAssert type(final MediaType type) throws Exception {
      doAssert(() -> assertEquals(type,
          MediaType.valueOf(response.getFirstHeader("Content-Type").getValue())));
      return this;
    }

    public void content(final String content) throws Exception {
      HttpEntity entity = response.getEntity();
      assertEquals(content, EntityUtils.toString(entity));
    }

    public void done() throws Exception {
      HttpEntity entity = response.getEntity();
      EntityUtils.consume(entity);
    }

    void doAssert(final HttpCall call) throws Exception {
      try {
        call.call();
      } catch (Exception ex) {
        HttpEntity entity = response.getEntity();
        EntityUtils.consume(entity);
        throw ex;
      }
    }
  }

  @Named("port")
  @Inject
  private int port;

  public void assertStatus(final Response.Status status, final HttpCall call) throws Exception {
    try {
      call.call();
      fail("expected " + status);
    } catch (HttpResponseException ex) {
      assertEquals(status.value(), ex.getStatusCode());
    }
  }

  protected URIBuilder uri(final String... parts) throws Exception {
    URIBuilder builder = new URIBuilder("http://localhost:" + port + "/"
        + Joiner.on("/").join(parts));
    return builder;
  }

  public HttpAssert assertHttp(final org.apache.http.client.fluent.Request request)
      throws Exception {
    HttpResponse response = request.execute().returnResponse();
    return new HttpAssert(response);
  }
}
