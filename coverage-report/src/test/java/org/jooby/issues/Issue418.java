package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http.MetaData.Request;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PushPromiseFrame;
import org.eclipse.jetty.http2.generator.Generator;
import org.eclipse.jetty.http2.parser.Parser;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.Utf8StringBuilder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jooby.MediaType;
import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class Issue418 extends ServerFeature {

  {
    http2();
    securePort(9443);

    get("/", req -> req.protocol() + (req.secure() ? ":secure" : ""));

    get("/app.js", () -> Results.ok("(function(){})()").type(MediaType.js));

    get("/push", req -> {
      req.push("/app.js");
      return "<html><script src=\"app.js\"></script><body>H2</body></html>";
    });

    get("/push-with-header", req -> {
      req.push("/app.js", ImmutableMap.of("etag", "123"));
      return "<html><script src=\"app.js\"></script><body>H2</body></html>";
    });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void h2c() throws Throwable {
    Map<Integer, Object> rsp = call("/", false);
    assertNotNull(rsp);
    Map<String, Object> stream = (Map<String, Object>) rsp.get(1);
    assertEquals(1, stream.get("streamId"));
    assertEquals("HTTP/2.0", stream.get("body"));
    assertEquals("8", stream.get("content-length"));
    assertEquals("text/html;charset=utf-8", stream.get("content-type"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void h2() throws Throwable {
    Map<Integer, Object> rsp = call("/", true);
    assertNotNull(rsp);
    Map<String, Object> stream = (Map<String, Object>) rsp.get(1);
    assertEquals(1, stream.get("streamId"));
    assertEquals("HTTP/2.0:secure", stream.get("body"));
    assertEquals("15", stream.get("content-length"));
    assertEquals("text/html;charset=utf-8", stream.get("content-type"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void h2push() throws Throwable {
    Map<Integer, Object> rsp = call("/push", true);
    assertNotNull(rsp);
    // stream 1
    Map<String, Object> stream1 = (Map<String, Object>) rsp.get(1);
    assertEquals(1, stream1.get("streamId"));
    assertEquals("<html><script src=\"app.js\"></script><body>H2</body></html>",
        stream1.get("body"));
    assertEquals("58", stream1.get("content-length"));
    assertEquals("text/html;charset=utf-8", stream1.get("content-type"));
    // stream 2
    Map<String, Object> stream2 = (Map<String, Object>) rsp.get(2);
    assertEquals(2, stream2.get("streamId"));
    assertEquals("(function(){})()", stream2.get("body"));
    assertEquals("16", stream2.get("content-length"));
    assertEquals("application/javascript;charset=utf-8", stream2.get("content-type"));
    Map<String, Object> pushPromise = (Map<String, Object>) stream2.get("push-promise");
    assertEquals(1, pushPromise.get("streamId"));
    assertEquals(2, pushPromise.get("promisedStreamId"));
    assertEquals("https://localhost:9443/app.js", pushPromise.get("uri"));
    assertEquals("GET", pushPromise.get("method"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void h2cpush() throws Throwable {
    Map<Integer, Object> rsp = call("/push", false);
    assertNotNull(rsp);
    // stream 1
    Map<String, Object> stream1 = (Map<String, Object>) rsp.get(1);
    assertEquals(1, stream1.get("streamId"));
    assertEquals("<html><script src=\"app.js\"></script><body>H2</body></html>",
        stream1.get("body"));
    assertEquals("58", stream1.get("content-length"));
    assertEquals("text/html;charset=utf-8", stream1.get("content-type"));
    // stream 2
    Map<String, Object> stream2 = (Map<String, Object>) rsp.get(2);
    assertEquals(2, stream2.get("streamId"));
    assertEquals("(function(){})()", stream2.get("body"));
    assertEquals("16", stream2.get("content-length"));
    assertEquals("application/javascript;charset=utf-8", stream2.get("content-type"));
    Map<String, Object> pushPromise = (Map<String, Object>) stream2.get("push-promise");
    assertEquals(1, pushPromise.get("streamId"));
    assertEquals(2, pushPromise.get("promisedStreamId"));
    assertEquals("http://localhost:9999/app.js", pushPromise.get("uri"));
    assertEquals("GET", pushPromise.get("method"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void h2pushWithHeader() throws Throwable {
    Map<Integer, Object> rsp = call("/push-with-header", true);
    assertNotNull(rsp);
    // stream 1
    Map<String, Object> stream1 = (Map<String, Object>) rsp.get(1);
    assertEquals(1, stream1.get("streamId"));
    assertEquals("<html><script src=\"app.js\"></script><body>H2</body></html>",
        stream1.get("body"));
    assertEquals("58", stream1.get("content-length"));
    assertEquals("text/html;charset=utf-8", stream1.get("content-type"));
    // stream 2
    Map<String, Object> stream2 = (Map<String, Object>) rsp.get(2);
    assertEquals(2, stream2.get("streamId"));
    assertEquals("(function(){})()", stream2.get("body"));
    assertEquals("16", stream2.get("content-length"));
    assertEquals("application/javascript;charset=utf-8", stream2.get("content-type"));
    Map<String, Object> pushPromise = (Map<String, Object>) stream2.get("push-promise");
    assertEquals(1, pushPromise.get("streamId"));
    assertEquals(2, pushPromise.get("promisedStreamId"));
    assertEquals("123", pushPromise.get("etag"));
    assertEquals("https://localhost:9443/app.js", pushPromise.get("uri"));
    assertEquals("GET", pushPromise.get("method"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void h2cpushWithHeader() throws Throwable {
    Map<Integer, Object> rsp = call("/push-with-header", false);
    assertNotNull(rsp);
    // stream 1
    Map<String, Object> stream1 = (Map<String, Object>) rsp.get(1);
    assertEquals(1, stream1.get("streamId"));
    assertEquals("<html><script src=\"app.js\"></script><body>H2</body></html>",
        stream1.get("body"));
    assertEquals("58", stream1.get("content-length"));
    assertEquals("text/html;charset=utf-8", stream1.get("content-type"));
    // stream 2
    Map<String, Object> stream2 = (Map<String, Object>) rsp.get(2);
    assertEquals(2, stream2.get("streamId"));
    assertEquals("(function(){})()", stream2.get("body"));
    assertEquals("16", stream2.get("content-length"));
    assertEquals("application/javascript;charset=utf-8", stream2.get("content-type"));
    Map<String, Object> pushPromise = (Map<String, Object>) stream2.get("push-promise");
    assertEquals(1, pushPromise.get("streamId"));
    assertEquals(2, pushPromise.get("promisedStreamId"));
    assertEquals("123", pushPromise.get("etag"));
    assertEquals("http://localhost:9999/app.js", pushPromise.get("uri"));
    assertEquals("GET", pushPromise.get("method"));
  }

  @Test
  public void http1_1_Upgrade() throws Throwable {
    // copy from:
    // https://github.com/eclipse/jetty.project/blob/master/jetty-http2/http2-server/src/test/java/org/eclipse/jetty/http2/server/HTTP2CServerTest.java#L116
    try (Socket client = new Socket("localhost", port)) {
      OutputStream output = client.getOutputStream();
      output.write(("" +
          "GET / HTTP/1.1\r\n" +
          "Host: localhost\r\n" +
          "Connection: upgrade, HTTP2-Settings\r\n" +
          "Upgrade: h2c\r\n" +
          "HTTP2-Settings: \r\n" +
          "\r\n").getBytes(StandardCharsets.UTF_8));
      output.flush();

      InputStream input = client.getInputStream();
      Utf8StringBuilder upgrade = new Utf8StringBuilder();
      int crlfs = 0;
      while (true) {
        int read = input.read();
        if (read == '\r' || read == '\n') {
          ++crlfs;
        } else {
          crlfs = 0;
        }
        upgrade.append((byte) read);
        if (crlfs == 4) {
          break;
        }
      }

      assertTrue(upgrade.toString().startsWith("HTTP/1.1 101 "));

      MappedByteBufferPool byteBufferPool = new MappedByteBufferPool();
      new Generator(byteBufferPool);

      final AtomicReference<HeadersFrame> headersRef = new AtomicReference<>();
      final AtomicReference<DataFrame> dataRef = new AtomicReference<>();
      final AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(2));
      Parser parser = new Parser(byteBufferPool, new Parser.Listener.Adapter() {
        @Override
        public void onHeaders(final HeadersFrame frame) {
          headersRef.set(frame);
          latchRef.get().countDown();
        }

        @Override
        public void onData(final DataFrame frame) {
          dataRef.set(frame);
          latchRef.get().countDown();
        }
      }, 4096, 8192);

      parseResponse(client, parser);

      assertTrue(latchRef.get().await(5, TimeUnit.SECONDS));

      HeadersFrame response = headersRef.get();
      assertNotNull(response);
      MetaData.Response responseMetaData = (MetaData.Response) response.getMetaData();
      assertEquals(200, responseMetaData.getStatus());

      DataFrame responseData = dataRef.get();
      assertNotNull(responseData);

      String content = BufferUtil.toString(responseData.getData());

      // The upgrade request is seen as HTTP/1.1.
      assertTrue(content, content.contains("HTTP/1.1"));
    }
  }

  @Test
  public void http1_1() throws Throwable {
    request()
        .get("/")
        .expect("HTTP/1.1");
  }

  @Test
  public void https1_1() throws Throwable {
    https()
        .get("/")
        .expect("HTTP/1.1:secure");
  }

  @SuppressWarnings("unchecked")
  private Map<Integer, Object> call(final String path, final boolean secure) throws Throwable {
    HTTP2Client client = new HTTP2Client();
    try {
      SslContextFactory sslContextFactory = new SslContextFactory(true);
      sslContextFactory.setIncludeCipherSuites("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
      client.addBean(sslContextFactory);
      client.start();

      String host = "localhost";
      int port = secure ? this.securePort : this.port;
      String scheme = secure ? "https" : "http";

      FuturePromise<Session> sessionPromise = new FuturePromise<>();
      client.connect(secure ? sslContextFactory : null, new InetSocketAddress(host, port),
          new ServerSessionListener.Adapter(), sessionPromise);
      Session session = sessionPromise.get();// 5, TimeUnit.SECONDS);

      HttpFields requestFields = new HttpFields();
      requestFields.put("User-Agent", client.getClass().getName() + "/" + Jetty.VERSION);
      MetaData.Request metaData = new MetaData.Request("GET",
          new HttpURI(scheme + "://" + host + ":" + port + path), HttpVersion.HTTP_2,
          requestFields);
      HeadersFrame frame = new HeadersFrame(metaData, null, true);
      final Phaser phaser = new Phaser(2);
      Map<Integer, Object> result = new HashMap<>();
      session.newStream(frame, new Promise.Adapter<>(), new Stream.Listener.Adapter() {
        @Override
        public void onHeaders(final Stream stream, final HeadersFrame frame) {
          Map<String, Object> body = stream(stream);
          result.put(stream.getId(), body);
          body.put("streamId", stream.getId());
          frame.getMetaData()
              .forEach(f -> body.put(f.getName().toLowerCase(), f.getValue().toLowerCase()));
          if (frame.isEndStream()) {
            phaser.arrive();
          }
        }

        private Map<String, Object> stream(final Stream stream) {
          Map<String, Object> data = (Map<String, Object>) result.get(stream.getId());
          if (data == null) {
            data = new HashMap<>();
            result.put(stream.getId(), data);
          }
          return data;
        }

        @Override
        public void onData(final Stream stream, final DataFrame frame, final Callback callback) {
          byte[] bytes = new byte[frame.getData().remaining()];
          frame.getData().get(bytes);
          Map<String, Object> body = stream(stream);
          body.put("body", new String(bytes));
          callback.succeeded();
          if (frame.isEndStream()) {
            phaser.arrive();
          }
        }

        @Override
        public Stream.Listener onPush(final Stream stream, final PushPromiseFrame frame) {
          Map<String, Object> body = stream(stream);
          MetaData md = frame.getMetaData();
          Map<String, Object> push = new HashMap<>();
          body.put("push-promise", push);
          md.forEach(f -> push.put(f.getName().toString(), f.getValue().toLowerCase()));
          MetaData.Request req = (Request) md;
          push.put("method", req.getMethod());
          push.put("uri", req.getURIString());
          push.put("streamId", frame.getStreamId());
          push.put("promisedStreamId", frame.getPromisedStreamId());
          phaser.register();
          return this;
        }
      });

      phaser.awaitAdvanceInterruptibly(phaser.arrive());// , 5, TimeUnit.SECONDS);

      return result;
    } finally {
      client.stop();
    }
  }

  protected boolean parseResponse(final Socket client, final Parser parser) throws IOException {
    return parseResponse(client, parser, 1000);
  }

  protected boolean parseResponse(final Socket client, final Parser parser, final long timeout)
      throws IOException {
    byte[] buffer = new byte[2048];
    InputStream input = client.getInputStream();
    client.setSoTimeout((int) timeout);
    while (true) {
      try {
        int read = input.read(buffer);
        if (read < 0) {
          return true;
        }
        parser.parse(ByteBuffer.wrap(buffer, 0, read));
        if (client.isClosed()) {
          return true;
        }
      } catch (SocketTimeoutException x) {
        return false;
      }
    }
  }
}
