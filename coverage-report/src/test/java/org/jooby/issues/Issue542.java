package org.jooby.issues;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.io.ByteStreams;

public class Issue542 extends ServerFeature {

  {
    assets("/assets/video.mp4");

    get("/no-len", req -> {
      return new ByteArrayInputStream("nolen".getBytes(StandardCharsets.UTF_8));
    });
  }

  @Test
  public void shouldGetFullContent() throws Exception {
    request()
        .get("/assets/video.mp4")
        .expect(200)
        .header("Content-Length", "383631");
  }

  @Test
  public void shouldGetPartialContent() throws Exception {
    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=0-99")
        .expect(206)
        .expect(bytes(0, 100))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 0-99/383631")
        .header("Content-Length", "100");

    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=100-199")
        .expect(206)
        .expect(bytes(100, 200))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 100-199/383631")
        .header("Content-Length", "100");
  }

  @Test
  public void shouldGetPartialContentByPrefix() throws Exception {
    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=0-")
        .expect(206)
        .expect(bytes(0, 383631))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 0-383630/383631")
        .header("Content-Length", "383631");
  }

  @Test
  public void shouldGetPartialContentBySuffix() throws Exception {
    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=-100")
        .expect(206)
        .expect(bytes(383631 - 100, 383631))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 383531-383630/383631")
        .header("Content-Length", "100");
  }

  @Test
  public void shouldGetPartialContentWhenEndExceed() throws Exception {
    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=383629-383632")
        .expect(206)
        .expect(bytes(383631 - 2, 383631))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 383629-383630/383631")
        .header("Content-Length", "2");
  }

  @Test
  public void shouldGet416OnInvalidRange() throws Exception {
    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=200-100")
        .expect(416)
        .header("Content-Range", "bytes */383631");

    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=x-100")
        .expect(416)
        .header("Content-Range", "bytes */383631");

    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=0-x")
        .expect(416)
        .header("Content-Range", "bytes */383631");

    request()
        .get("/assets/video.mp4")
        .header("Range", "bytes=")
        .expect(416)
        .header("Content-Range", "bytes */383631");
  }

  @Test
  public void shouldGet416UnknownRange() throws Exception {
    request()
        .get("/assets/video.mp4")
        .header("Range", "foo")
        .expect(416)
        .header("Content-Range", "bytes */383631");
  }

  private byte[] bytes(final int offset, final int len) throws IOException {
    try (InputStream stream = getClass().getResourceAsStream("/assets/video.mp4")) {
      byte[] range = new byte[len - offset];
      byte[] bytes = ByteStreams.toByteArray(stream);
      System.arraycopy(bytes, offset, range, 0, len - offset);
      return range;
    }
  }

}
