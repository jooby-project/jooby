package org.jooby;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class BuiltinParserFormatterFeature extends ServerFeature {

  {
    get("/stream", () -> new ByteArrayInputStream("stream".getBytes()));

    get("/bytes", () -> "bytes".getBytes());

    get("/direct-buffer", () -> {
      ByteBuffer buffer = ByteBuffer.allocateDirect("direct-buffer".length());
      buffer.put("direct-buffer".getBytes());
      return buffer;
    });

    get("/reader", () -> new StringReader("reader"));
  }

  @Test
  public void stream() throws Exception {
    request()
        .get("/stream")
        .expect(200)
        .expect("stream")
        .header("Content-Length", "6")
        .header("Content-Type", "application/octet-stream");
  }

  @Test
  public void bytes() throws Exception {
    request()
        .get("/bytes")
        .expect(200)
        .expect("bytes")
        .header("Content-Length", "5")
        .header("Content-Type", "application/octet-stream");
  }

  @Test
  public void directBuffer() throws Exception {
    request()
        .get("/direct-buffer")
        .expect(200)
        .expect("direct-buffer")
        .header("Content-Length", "13")
        .header("Content-Type", "application/octet-stream");
  }

  @Test
  public void reader() throws Exception {
    request()
        .get("/reader")
        .expect(200)
        .expect("reader")
        .header("Content-Length", "6")
        .header("Content-Type", "text/html;charset=UTF-8");

  }

}
