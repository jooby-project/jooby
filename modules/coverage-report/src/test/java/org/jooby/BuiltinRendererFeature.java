package org.jooby;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class BuiltinRendererFeature extends ServerFeature {

  {

    get("/stream", () -> new ByteArrayInputStream("stream".getBytes()));

    get("/bytes", () -> "bytes".getBytes());

    get("/cbuffer", () -> CharBuffer.wrap("cbuffer"));

    get("/file", () -> new File("src/test/resources/"
        + BuiltinRendererFeature.class.getName().replace('.', '/') + ".txt"));

    get("/fchannel", () -> new FileInputStream(new File("src/test/resources/"
        + BuiltinRendererFeature.class.getName().replace('.', '/') + ".txt")).getChannel());

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
        .header("Content-Length", 6)
        .header("Content-Type", "application/octet-stream");
  }

  @Test
  public void reader() throws Exception {
    request()
        .get("/reader")
        .expect(200)
        .expect("reader")
        .header("Content-Length", 6)
        .header("Content-Type", "text/html;charset=utf-8");

    request()
        .get("/ereader")
        .expect(200)
        .expect("")
        .header("Content-Length", 0)
        .header("Content-Type", "text/html;charset=utf-8");
  }

  @Test
  public void file() throws Exception {
    request()
        .get("/file")
        .expect(200)
        .expect("reader")
        .header("Content-Length", 6)
        .header("Content-Type", "text/plain;charset=utf-8");
  }

  @Test
  public void fchannel() throws Exception {
    request()
        .get("/fchannel")
        .expect(200)
        .expect("reader")
        .header("Content-Length", 6)
        .header("Content-Type", "application/octet-stream");
  }

  @Test
  public void cbuffer() throws Exception {
    request()
        .get("/cbuffer")
        .expect(200)
        .expect("cbuffer")
        .header("Content-Length", 7)
        .header("Content-Type", "text/html;charset=utf-8");
  }

  @Test
  public void bytes() throws Exception {
    request()
        .get("/bytes")
        .expect(200)
        .expect("bytes")
        .header("Content-Length", 5)
        .header("Content-Type", "application/octet-stream");
  }

  @Test
  public void directBuffer() throws Exception {
    request()
        .get("/direct-buffer")
        .expect(200)
        .expect("direct-buffer")
        .header("Content-Length", 13)
        .header("Content-Type", "application/octet-stream");
  }

}
