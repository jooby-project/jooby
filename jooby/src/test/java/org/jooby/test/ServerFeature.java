package org.jooby.test;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jooby.Jooby;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;

@RunWith(JoobySuite.class)
public abstract class ServerFeature extends Jooby {

  public static class Server {

    public interface Callback {

      void execute(String value) throws Exception;
    }

    public interface ServerCallback {

      void execute(Server request) throws Exception;
    }

    public static class Request {
      private Executor executor;

      private org.apache.http.client.fluent.Request req;

      private org.apache.http.HttpResponse rsp;

      private Server server;

      public Request(final Server server, final Executor executor,
          final org.apache.http.client.fluent.Request req) {
        this.server = server;
        this.executor = executor;
        this.req = req;
      }

      private Response execute() throws Exception {
        this.rsp = executor.execute(req).returnResponse();
        return new Response(server, rsp);
      }

      public Response expect(final String content) throws Exception {
        return execute().expect(content);
      }

      public Response expect(final Callback callback) throws Exception {
        return execute().expect(callback);
      }

      public Response expect(final int status) throws Exception {
        return execute().expect(status);
      }

      public Response expect(final byte[] content) throws Exception {
        return execute().expect(content);
      }

      public Request header(final String name, final Object value) {
        req.addHeader(name, value.toString());
        return this;
      }

      public Body multipart() {
        return new Body(MultipartEntityBuilder.create(), this);
      }

      public Body form() {
        return new Body(this);
      }

      public void close() {
        EntityUtils.consumeQuietly(rsp.getEntity());
      }

      public Request body(final String body, final String type) {
        req.bodyString(body, ContentType.parse(type));
        return this;
      }

    }

    public static class Body {

      private Request req;

      private MultipartEntityBuilder parts;

      private List<BasicNameValuePair> fields;

      public Body(final MultipartEntityBuilder parts, final Request req) {
        this.parts = parts;
        this.req = req;
      }

      public Body(final Request req) {
        this.fields = new ArrayList<>();
        this.req = req;
      }

      public Response expect(final String content) throws Exception {
        if (parts != null) {
          req.req.body(parts.build());
        } else {
          req.req.bodyForm(fields);
        }
        return req.expect(content);
      }

      public Response expect(final int status) throws Exception {
        if (parts != null) {
          req.req.body(parts.build());
        } else {
          req.req.bodyForm(fields);
        }
        return req.expect(status);
      }

      public Body add(final String name, final Object value, final String type) {
        if (parts != null) {
          parts.addTextBody(name, value.toString(), ContentType.parse(type));
        } else {
          fields.add(new BasicNameValuePair(name, value.toString()));
        }
        return this;
      }

      public Body add(final String name, final Object value) {
        return add(name, value, "text/plain");
      }

      public Body file(final String name, final byte[] bytes, final String type,
          final String filename) {
        if (parts != null) {
          parts.addBinaryBody(name, bytes, ContentType.parse(type), filename);
        } else {
          throw new IllegalStateException("Not a multipart");
        }
        return this;
      }

    }

    public static class Response {

      private Server server;

      private HttpResponse rsp;

      public Response(final Server server, final org.apache.http.HttpResponse rsp) {
        this.server = server;
        this.rsp = rsp;
      }

      public Response expect(final String content) throws Exception {
        assertEquals(content, EntityUtils.toString(this.rsp.getEntity()));
        return this;
      }

      public Response expect(final int status) throws Exception {
        assertEquals(status, rsp.getStatusLine().getStatusCode());
        return this;
      }

      public Response expect(final byte[] content) throws Exception {
        assertArrayEquals(content, EntityUtils.toByteArray(this.rsp.getEntity()));
        return this;
      }

      public Response expect(final Callback callback) throws Exception {
        callback.execute(EntityUtils.toString(this.rsp.getEntity()));
        return this;
      }

      public Response header(final String headerName, final String headerValue)
          throws Exception {
        if (headerValue == null) {
          assertNull(rsp.getFirstHeader(headerName));
        } else {
          Header header = rsp.getFirstHeader(headerName);
          if (header == null) {
            // friendly junit err
            assertEquals(headerValue, header);
          } else {
            assertEquals(headerValue.toLowerCase(), header.getValue().toLowerCase());
          }
        }
        return this;
      }

      public Response headers(final BiConsumer<String, String> headers)
          throws Exception {
        for (Header header : rsp.getAllHeaders()) {
          headers.accept(header.getName(), header.getValue());
        }
        return this;
      }

      public Response header(final String headerName, final Object headerValue)
          throws Exception {
        if (headerValue == null) {
          return header(headerName, (String) null);
        } else {
          return header(headerName, headerValue.toString());
        }
      }

      public Response header(final String headerName, final Optional<Object> headerValue)
          throws Exception {
        Header header = rsp.getFirstHeader(headerName);
        if (header != null) {
          assertEquals(headerValue.get(), header.getValue());
        }
        return this;
      }

      public Response header(final String headerName, final Callback callback) throws Exception {
        callback.execute(rsp.getFirstHeader(headerName).getValue());
        return this;
      }

      public Response empty() throws Exception {
        HttpEntity entity = this.rsp.getEntity();
        if (entity != null) {
          assertEquals("", EntityUtils.toString(entity));
        }
        return this;
      }

      public void request(final ServerCallback request) throws Exception {
        request.execute(server);
      }

      public void startsWith(final String value) throws IOException {
        assertTrue(EntityUtils.toString(this.rsp.getEntity()).startsWith(value));
      }

    }

    private Executor executor;

    private CloseableHttpClient client;

    private BasicCookieStore cookieStore;

    private String host;

    private Request req;

    private HttpClientBuilder builder;

    public Server(final String host) {
      this.host = host;

      this.cookieStore = new BasicCookieStore();
      this.builder = HttpClientBuilder.create()
          .setMaxConnTotal(1)
          .setRetryHandler(new StandardHttpRequestRetryHandler(0, false))
          .setMaxConnPerRoute(1)
          .setDefaultCookieStore(cookieStore);
    }

    public Server resetCookies() {
      cookieStore.clear();
      return this;
    }

    public Server dontFollowRedirect() {
      builder.setRedirectStrategy(new RedirectStrategy() {

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
      });
      return this;
    }

    public Request get(final String path) {
      this.req = new Request(this, executor(), org.apache.http.client.fluent.Request.Get(host
          + path));
      return req;
    }

    public Request trace(final String path) {
      this.req = new Request(this, executor(), org.apache.http.client.fluent.Request.Trace(host
          + path));
      return req;
    }

    public Request options(final String path) {
      this.req = new Request(this, executor(), org.apache.http.client.fluent.Request.Options(host
          + path));
      return req;
    }

    public Request head(final String path) {
      this.req = new Request(this, executor(), org.apache.http.client.fluent.Request.Head(host
          + path));
      return req;
    }

    private Executor executor() {
      if (executor == null) {
        client = builder.build();
        executor = Executor.newInstance(client);
      }
      return executor;
    }

    public Request post(final String path) {
      this.req = new Request(this, executor(), org.apache.http.client.fluent.Request.Post(host
          + path));
      return req;
    }

    public Request put(final String path) {
      this.req = new Request(this, executor(), org.apache.http.client.fluent.Request.Put(host
          + path));
      return req;
    }

    public Request delete(final String path) {
      this.req = new Request(this, executor(), org.apache.http.client.fluent.Request.Delete(host
          + path));
      return req;
    }

    public Request patch(final String path) {
      this.req = new Request(this, executor(), org.apache.http.client.fluent.Request.Patch(host
          + path));
      return req;
    }

    void stop() throws IOException {
      if (this.req != null) {
        this.req.close();
      }
      if (client != null) {
        client.close();
      }
    }

  }

  public static boolean DEBUG = false;

  @Named("port")
  @Inject
  protected int port;

  private Server server = null;

  @Before
  public void debug() {
    if (DEBUG) {
      java.util.logging.Logger.getLogger("httpclient.wire.header").setLevel(
          java.util.logging.Level.FINEST);
      java.util.logging.Logger.getLogger("httpclient.wire.content").setLevel(
          java.util.logging.Level.FINEST);

      System.setProperty("org.apache.commons.logging.Log",
          "org.apache.commons.logging.impl.SimpleLog");
      System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
      System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers",
          "debug");
    }
  }

  @Before
  public void createServer() {
    checkState(server == null, "Server was created already");
    server = new Server("http://localhost:" + port);
  }

  @After
  public void stopServer() throws IOException {
    checkState(server != null, "Server wasn't started");
    server.stop();
  }

  public Server request() {
    checkState(server != null, "Server wasn't started");
    return server;
  }

  protected URIBuilder ws(final String... parts) throws Exception {
    URIBuilder builder = new URIBuilder("ws://localhost:" + port + "/"
        + Joiner.on("/").join(parts));
    return builder;
  }

}
