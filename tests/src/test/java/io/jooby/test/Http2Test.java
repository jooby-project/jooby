/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.google.common.collect.ImmutableMap;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Http2Test {

  @ServerTest
  public void http2(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setServerOptions(new ServerOptions().setHttp2(true).setSecurePort(8443));

              app.get(
                  "/",
                  ctx ->
                      ImmutableMap.of(
                          "secure",
                          ctx.isSecure(),
                          "protocol",
                          ctx.getProtocol(),
                          "scheme",
                          ctx.getScheme()));
            })
        .ready(
            (http, https) -> {
              https.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "{secure=true, protocol=HTTP/2.0, scheme=https}", rsp.body().string());
                  });
              http.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "{secure=false, protocol=HTTP/1.1, scheme=http}", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void http2c(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setServerOptions(new ServerOptions().setHttp2(true));

              app.get(
                  "/",
                  ctx ->
                      ImmutableMap.of(
                          "secure",
                          ctx.isSecure(),
                          "protocol",
                          ctx.getProtocol(),
                          "scheme",
                          ctx.getScheme()));
            })
        .ready(
            http -> {
              http.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "{secure=false, protocol=HTTP/1.1, scheme=http}", rsp.body().string());
                  });

              hc2(
                  http,
                  "/",
                  rsp -> {
                    assertEquals(
                        "{secure=false, protocol=HTTP/2.0, scheme=http}", rsp.body().string());
                  });
            });
  }

  private void hc2(WebClient http, String path, SneakyThrows.Consumer<Response> consumer)
      throws ExecutionException, InterruptedException {
    HttpFields requestFields = HttpFields.build();
    HttpURI uri = HttpURI.from("http://localhost:" + http.getPort() + path);
    MetaData.Request metaData = new MetaData.Request("GET", uri, HttpVersion.HTTP_2, requestFields);
    HeadersFrame frame = new HeadersFrame(metaData, null, true);
    final Phaser phaser = new Phaser(2);
    FuturePromise<org.eclipse.jetty.http2.api.Session> sessionPromise = new FuturePromise<>();
    h2c.connect(
        null,
        new InetSocketAddress("localhost", http.getPort()),
        new ServerSessionListener() {
          @Override
          public void onAccept(Session session) {}
        },
        sessionPromise);
    Session session = sessionPromise.get();
    Response.Builder builder = new Response.Builder();
    session.newStream(
        frame,
        new Promise.Adapter<>(),
        new Stream.Listener() {
          @Override
          public void onHeaders(final Stream stream, final HeadersFrame frame) {
            MetaData.Response md = (MetaData.Response) frame.getMetaData();
            StatusCode statusCode = StatusCode.valueOf(md.getStatus());
            builder.code(statusCode.value()).message(statusCode.reason());
            md.forEach(
                f -> builder.addHeader(f.getName().toLowerCase(), f.getValue().toLowerCase()));
            if (frame.isEndStream()) {
              phaser.arrive();
            } else {
              stream.demand();
            }
          }

          @Override
          public void onDataAvailable(Stream stream) {
            while (true) {
              Stream.Data data = stream.readData();
              if (data == null) {
                stream.demand();
                return;
              }
              var buf = data.frame().getByteBuffer();
              byte[] bytes = new byte[buf.remaining()];
              buf.get(bytes);
              ResponseBody responseBody = ResponseBody.create(bytes, MediaType.parse("text/plain"));
              builder.body(responseBody);
              if (frame.isEndStream()) {
                phaser.arrive();
              }
              data.release();
              if (data.frame().isEndStream()) return;
            }
          }
        });

    phaser.awaitAdvanceInterruptibly(phaser.arrive());

    consumer.accept(
        builder
            .request(new Request.Builder().url(uri.toString()).method("GET", null).build())
            .protocol(Protocol.HTTP_2)
            .build());
  }

  private static HTTP2Client h2c;

  @BeforeAll
  public static void init() throws Exception {
    h2c = new HTTP2Client();
    h2c.start();
  }

  @AfterAll
  public static void close() throws Exception {
    h2c.stop();
  }
}
