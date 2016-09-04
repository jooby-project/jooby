package org.jooby.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.rules.ExternalResource;

import com.google.common.base.Throwables;

public class Client extends ExternalResource {

  public interface Callback {

    void execute(String value) throws Exception;
  }

  public interface ArrayCallback {

    void execute(String[] values) throws Exception;
  }

  public interface ServerCallback {

    void execute(Client request) throws Exception;
  }

  public static class Request {
    private Executor executor;

    private org.apache.http.client.fluent.Request req;

    private org.apache.http.HttpResponse rsp;

    private Client server;

    public Request(final Client server, final Executor executor,
        final org.apache.http.client.fluent.Request req) {
      this.server = server;
      this.executor = executor;
      this.req = req;
    }

    public Response execute() throws Exception {
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
      if (type == null) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        HttpEntity entity = new InputStreamEntity(new ByteArrayInputStream(bytes), bytes.length);
        req.body(entity);
      } else {
        req.bodyString(body, ContentType.parse(type));
      }
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

    private Client server;

    private HttpResponse rsp;

    public Response(final Client server, final org.apache.http.HttpResponse rsp) {
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
      callback.execute(
          Optional.ofNullable(rsp.getFirstHeader(headerName))
              .map(Header::getValue)
              .orElse(null));
      return this;
    }

    public Response headers(final String headerName, final ArrayCallback callback)
        throws Exception {
      Header[] headers = rsp.getHeaders(headerName);
      String[] values = new String[headers.length];
      for (int i = 0; i < values.length; i++) {
        values[i] = headers[i].getValue();
      }
      callback.execute(values);
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
      String rsp = EntityUtils.toString(this.rsp.getEntity());
      if (!rsp.startsWith(value)) {
        assertEquals(value, rsp);
      }
    }

  }

  private Executor executor;

  private CloseableHttpClient client;

  private BasicCookieStore cookieStore;

  private String host;

  private Request req;

  private HttpClientBuilder builder;

  private UsernamePasswordCredentials creds;

  public Client(final String host) {
    this.host = host;
  }

  public Client() {
    this("http://localhost:8080");
  }

  public void start() {
    this.cookieStore = new BasicCookieStore();
    this.builder = HttpClientBuilder.create()
        .setMaxConnTotal(1)
        .setRetryHandler(new StandardHttpRequestRetryHandler(0, false))
        .setMaxConnPerRoute(1)
        .setDefaultCookieStore(cookieStore);
  }

  public Client resetCookies() {
    cookieStore.clear();
    return this;
  }

  public Client dontFollowRedirect() {
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
      if (this.host.startsWith("https://")) {
        try {
          SSLContext sslContext = SSLContexts.custom()
              .loadTrustMaterial(null, (chain, authType) -> true)
              .useTLS()
              .build();
          builder.setSslcontext(sslContext);
          builder.setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (Exception ex) {
          Throwables.propagate(ex);
        }
      }
      client = builder.build();
      executor = Executor.newInstance(client);
      if (creds != null) {
        executor.auth(creds);
      }
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
    this.req = new Request(this, executor(), pathHack(host + path));
    return req;
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private org.apache.http.client.fluent.Request pathHack(final String string) {
    try {
      // Patch is available since 4.4, but we are in 4.3 because of AWS-SDK
      Class ireqclass = getClass().getClassLoader().loadClass(
          "org.apache.http.client.fluent.InternalHttpRequest");
      Constructor<org.apache.http.client.fluent.Request> constructor = org.apache.http.client.fluent.Request.class
          .getDeclaredConstructor(ireqclass);
      constructor.setAccessible(true);

      Constructor ireqcons = ireqclass.getDeclaredConstructor(String.class, URI.class);
      ireqcons.setAccessible(true);
      Object ireq = ireqcons.newInstance("PATCH", URI.create(string));
      return constructor.newInstance(ireq);
    } catch (NoSuchMethodException | SecurityException | ClassNotFoundException
        | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException ex) {
      throw new UnsupportedOperationException(ex);
    }
  }

  public void stop() throws IOException {
    if (this.req != null) {
      try {
        this.req.close();
      } catch (NullPointerException ex) {
      }
    }
    if (client != null) {
      client.close();
    }
    this.builder = null;
    this.executor = null;
  }

  public Client basic(final String username, final String password) {
    creds = new UsernamePasswordCredentials(username, password);
    return this;
  }

  @Override
  protected void before() throws Throwable {
    start();
  }

  @Override
  protected void after() {
    try {
      stop();
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to stop client", ex);
    }
  }
}
