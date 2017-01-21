package org.jooby;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class BufferSizeFeature extends ServerFeature {

  int len = 1024;

  {
    use(ConfigFactory.empty()
        .withValue("server.http.ResponseBufferSize", ConfigValueFactory.fromAnyRef(len / 2)));

    get("/", req -> {
      String value = req.param("data").value();
      Optional<Long> len = req.param("len").toOptional(Long.class);
      Optional<Boolean> chunked = req.param("chunked").toOptional(Boolean.class);
      Result result = Results.ok(value);
      if (!req.param("short").toOptional().isPresent()) {
        result = Results.ok(new ByteArrayInputStream(value.getBytes(req.charset())));
      }
      Result rsp = result;
      len.ifPresent(l -> rsp.header("Content-Length", l));
      chunked.ifPresent(c -> rsp.header("Transfer-Encoding", "chunked"));
      return rsp;
    });
  }

  @Test
  public void largeResponsesFallbackToTransferEncodingChunked() throws Exception {
    String data = rnd(len);
    request()
        .get("/?data=" + data)
        .expect(data)
        .header("Transfer-Encoding", "chunked");
  }

  @Test
  public void largeResponsesWithContentLengthMustBeKept() throws Exception {
    String data = rnd(len);
    int len = len(data);
    request()
        .get("/?data=" + data + "&len=" + len)
        .expect(data)
        .header("Content-Length", len);

  }

  @Test
  public void largeResponsesWithTransferEncodingMustBeKept() throws Exception {
    String data = rnd(len * 2);
    request()
        .get("/?data=" + data + "&chunked=true")
        .expect(data)
        .header("Transfer-Encoding", "chunked");

  }

  @Test
  public void shortResponseShouldGuessContentLength() throws Exception {
    request()
        .get("/?data=hello&short=yes")
        .expect("hello")
        .header("Content-Length", "5");
  }

  @Test
  public void shortResponseShouldKeepContentLength() throws Exception {
    request()
        .get("/?data=hello&len=5")
        .expect("hello")
        .header("Content-Length", "5");
  }

  @Test
  public void shortResponseShouldPreferContentLengthEvenIfTransferEncodingChunkWasSet()
      throws Exception {
    request()
        .get("/?data=hello&chunked=true&short=yes")
        .expect("hello")
        .header("Content-Length", "5");
  }

  public String rnd(final int len) {
    StringBuilder buffer = new StringBuilder();
    int cursor = 0;
    int max = 91 - 65;
    for (int i = 0; i < len; i++) {
      char ch = (char) (cursor + 65);
      buffer.append(ch);
      cursor = (cursor + 1) % max;
    }
    return buffer.toString();
  }

  private int len(final String data) {
    return data.getBytes(Charsets.UTF_8).length;
  }
}
