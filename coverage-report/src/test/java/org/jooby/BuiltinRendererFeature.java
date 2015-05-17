package org.jooby;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class BuiltinRendererFeature extends ServerFeature {

  {
    get("/stream", () -> new ByteArrayInputStream("stream".getBytes()));

    get("/bytes", () -> "bytes".getBytes());

    get("/reader",
        () -> new InputStreamReader(getClass().getResourceAsStream("BuiltinRendererFeature.txt")));

    get("/ereader",
        () -> new InputStreamReader(getClass().getResourceAsStream("BuiltinRendererFeature.empty")));

    get("/direct-buffer", () -> {
      ByteBuffer buffer = ByteBuffer.allocateDirect("direct-buffer".length());
      buffer.put("direct-buffer".getBytes());
      buffer.flip();
      return buffer;
    });
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
  public void reader() throws Exception {
    request()
        .get("/reader")
        .expect(200)
        .expect("reader")
        .header("Content-Length", "6")
        .header("Content-Type", "text/html;charset=utf-8");

    request()
        .get("/ereader")
        .expect(200)
        .expect("")
        .header("Content-Length", 0)
        .header("Content-Type", "text/html;charset=utf-8");
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

}
