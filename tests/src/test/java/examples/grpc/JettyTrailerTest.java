/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.grpc;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.http.*;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Promise;
import org.junit.jupiter.api.Test;

public class JettyTrailerTest {

  @Test
  public void testEmptyWriteWithTrailersCausesUnexpectedEOS() throws Exception {
    // 1. Setup Server
    Server server = new Server();
    HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory();
    ServerConnector connector = new ServerConnector(server, h2);
    connector.setPort(0);
    server.addConnector(connector);

    server.setHandler(
        new Handler.Abstract() {
          @Override
          public boolean handle(Request request, Response response, Callback callback) {
            // Set the trailers that gRPC-Java expects
            response.setTrailersSupplier(() -> HttpFields.build().put("grpc-status", "0"));

            // The core of the issue: sending a "last" write with no data
            // This triggers Data.eof() in Jetty 12 instead of a HEADERS frame
            response.write(true, null, callback);
            return true;
          }
        });
    server.start();

    // 2. Setup Client
    HTTP2Client client = new HTTP2Client();
    client.start();

    CompletableFuture<Void> resultFuture = new CompletableFuture<>();
    InetSocketAddress address = new InetSocketAddress("localhost", connector.getLocalPort());

    client.connect(
        address,
        new Session.Listener() {},
        new Promise<>() {
          @Override
          public void succeeded(Session session) {
            HttpURI uri = HttpURI.from("http://localhost:" + connector.getLocalPort() + "/");
            MetaData.Request metaData =
                new MetaData.Request("GET", uri, HttpVersion.HTTP_2, HttpFields.build());
            HeadersFrame requestFrame = new HeadersFrame(metaData, null, true);

            session.newStream(
                requestFrame,
                Promise.noop(),
                new Stream.Listener() {
                  @Override
                  public void onDataAvailable(Stream stream) {
                    // Access the Data wrapper you identified
                    Stream.Data data = stream.readData();
                    if (data != null) {
                      DataFrame frame = data.frame();

                      // If we receive an empty DATA frame with END_STREAM,
                      // Jetty has closed the stream before sending trailers.
                      if (frame.isEndStream()) {
                        resultFuture.completeExceptionally(
                            new RuntimeException(
                                "BUG REPRODUCED: Received Data.EOF (DATA frame with END_STREAM)."
                                    + " This violates gRPC protocol as trailers in HEADERS were"
                                    + " expected."));
                        return;
                      }
                    }
                    stream.demand();
                  }

                  @Override
                  public void onHeaders(Stream stream, HeadersFrame frame) {
                    MetaData metaData = frame.getMetaData();
                    HttpFields fields = metaData.getHttpFields();
                    // If trailers arrive correctly with END_STREAM, the test passes
                    if (frame.isEndStream() && fields.contains("grpc-status")) {
                      resultFuture.complete(null);
                    }
                  }

                  @Override
                  public void onFailure(
                      Stream stream,
                      int error,
                      String reason,
                      Throwable failure,
                      Callback callback) {
                    resultFuture.completeExceptionally(failure);
                    callback.succeeded();
                  }
                });
          }

          @Override
          public void failed(Throwable x) {
            resultFuture.completeExceptionally(x);
          }
        });

    // 3. Evaluation
    try {
      // We expect this to time out or fail if the bug exists
      resultFuture.get(5, TimeUnit.SECONDS);
      System.out.println("SUCCESS: Trailers arrived correctly on a HEADERS frame.");
    } catch (Exception e) {
      // In the bug scenario, e.getCause() will contain our RuntimeException
      String message = e.getMessage() != null ? e.getMessage() : e.getCause().getMessage();
      fail("Test failed due to Jetty behavior: " + message);
    } finally {
      server.stop();
      client.stop();
    }
  }

  @Test
  public void testBidiExchangeWithTrailers() throws Exception {
    // 1. Setup Server
    Server server = new Server();
    ServerConnector connector = new ServerConnector(server, new HTTP2ServerConnectionFactory());
    server.addConnector(connector);

    server.setHandler(
        new Handler.Abstract() {
          @Override
          public boolean handle(Request request, Response response, Callback callback) {
            // Set trailers supplier
            response.setTrailersSupplier(() -> HttpFields.build().put("grpc-status", "0"));

            // Obtain the content source to read client data

            request.demand(
                () -> {
                  var chunk = request.read();
                  if (chunk != null) {
                    // If we received data from client
                    if (BufferUtil.hasContent(chunk.getByteBuffer())) {
                      // Send an echo response back
                      var echo = ByteBuffer.wrap("Server Echo".getBytes(StandardCharsets.UTF_8));
                      response.write(false, echo, Callback.NOOP);
                    }

                    // Check if this was the last chunk from client
                    if (chunk.isLast()) {
                      // SIGNAL END OF SERVER STREAM
                      // This triggers the Data.EOF bug in 12.1.5
                      response.write(true, null, callback);
                    }
                    chunk.release();
                  }
                });
            return true;
          }
        });
    server.start();

    // 2. Setup Client
    HTTP2Client client = new HTTP2Client();
    client.start();
    CompletableFuture<Void> resultFuture = new CompletableFuture<>();
    int port = connector.getLocalPort();

    client.connect(
        new InetSocketAddress("localhost", port),
        new Session.Listener() {},
        new Promise<>() {
          @Override
          public void succeeded(Session session) {
            HttpURI uri = HttpURI.from("http://localhost:" + port + "/");
            MetaData.Request metaData =
                new MetaData.Request("POST", uri, HttpVersion.HTTP_2, HttpFields.build());

            // Client starts stream
            HeadersFrame headers = new HeadersFrame(metaData, null, false);

            session.newStream(
                headers,
                Promise.noop(),
                new Stream.Listener() {
                  @Override
                  public void onDataAvailable(Stream stream) {
                    Stream.Data data = stream.readData();
                    if (data != null) {
                      if (data.frame().isEndStream()) {
                        // If Jetty sends a DATA frame with END_STREAM, the bug is reproduced
                        resultFuture.completeExceptionally(
                            new RuntimeException(
                                "Received DATA frame with END_STREAM flag. Expected Trailers in"
                                    + " HEADERS."));
                        return;
                      }
                    }
                    stream.demand();
                  }

                  @Override
                  public void onHeaders(Stream stream, HeadersFrame frame) {
                    HttpFields fields = frame.getMetaData().getHttpFields();
                    if (frame.isEndStream() && fields.contains("grpc-status")) {
                      resultFuture.complete(null);
                    }
                  }
                });

            // Client sends one message and closes client-side stream
            session
                .getStreams()
                .forEach(
                    s -> {
                      ByteBuffer clientMsg =
                          ByteBuffer.wrap("Client Hello".getBytes(StandardCharsets.UTF_8));
                      s.data(new DataFrame(s.getId(), clientMsg, true), Callback.NOOP);
                    });
          }
        });

    // 3. Evaluation
    try {
      resultFuture.get(5, TimeUnit.SECONDS);
      System.out.println("SUCCESS: Bidi exchange completed correctly.");
    } catch (Exception e) {
      String msg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
      fail("Reproduced: " + msg);
    } finally {
      server.stop();
      client.stop();
    }
  }
}
