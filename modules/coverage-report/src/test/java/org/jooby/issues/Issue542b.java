package org.jooby.issues;

import java.io.IOException;
import java.io.InputStream;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.io.ByteStreams;

public class Issue542b extends ServerFeature {

  {
    assets("/webjars/**", "/META-INF/resources/webjars/{0}");
  }

  @Test
  public void shouldGetFullContent() throws Exception {
    request()
        .get("/webjars/jquery/2.2.4/jquery.js")
        .expect(200)
        .header("Content-Length", "257551");
  }

  @Test
  public void shouldGetPartialContent() throws Exception {
    request()
        .get("/webjars/jquery/2.2.4/jquery.js")
        .header("Range", "bytes=0-99")
        .expect(206)
        .expect(bytes(0, 100))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 0-99/257551")
        .header("Content-Length", "100");

    request()
        .get("/webjars/jquery/2.2.4/jquery.js")
        .header("Range", "bytes=100-199")
        .expect(206)
        .expect(bytes(100, 200))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 100-199/257551")
        .header("Content-Length", "100");
  }

  @Test
  public void shouldGetPartialContentByPrefix() throws Exception {
    request()
        .get("/webjars/jquery/2.2.4/jquery.js")
        .header("Range", "bytes=0-")
        .expect(206)
        .expect(bytes(0, 257551))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 0-247386/257551")
        .header("Content-Length", "257551");
  }

  @Test
  public void shouldGetPartialContentBySuffix() throws Exception {
    request()
        .get("/webjars/jquery/2.2.4/jquery.js")
        .header("Range", "bytes=-100")
        .expect(206)
        .expect(bytes(257551 - 100, 257551))
        .header("Accept-Ranges", "bytes")
        .header("Content-Range", "bytes 247287-247386/257551")
        .header("Content-Length", "100");
  }

  @Test
  public void shouldGet416() throws Exception {
    request()
        .get("/webjars/jquery/2.2.4/jquery.js")
        .header("Range", "bytes=200-100")
        .expect(416)
        .header("Content-Range", "bytes */257551");
  }

  private byte[] bytes(final int offset, final int len) throws IOException {
    try (InputStream stream = getClass()
        .getResourceAsStream("/META-INF/resources/webjars/jquery/2.2.4/jquery.js")) {
      byte[] range = new byte[len - offset];
      byte[] bytes = ByteStreams.toByteArray(stream);
      System.arraycopy(bytes, offset, range, 0, len - offset);
      return range;
    }
  }

}
