package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1656 {

  @ServerTest
  public void gzip(ServerTestRunner runner) {
    runner.define(app -> {
      app.setServerOptions(new ServerOptions().setGzip(true));
      app.assets("/static/*", "/files");
    }).ready(client -> {
      client.get("/static/fileupload.js").prepare(req -> {
        req.addHeader("Accept-Encoding", "gzip");
      }).execute(rsp -> {
        ResponseBody body = rsp.body();
        long len = body.contentLength();
        if (len == -1) {
          assertEquals("chunked", rsp.header("Transfer-Encoding"));
        } else {
          assertEquals(63, rsp.body().contentLength());
        }
        assertEquals("gzip", rsp.header("content-encoding"));
        assertEquals("(function () {  console.log('ready');})();", ungzip(body.bytes()).trim());
      });
    });
  }

  private String ungzip(byte[] buff) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(buff));
    Scanner scanner = new Scanner(gzip);
    StringBuilder str = new StringBuilder();
    while (scanner.hasNext()) {
      str.append(scanner.nextLine());
    }
    return str.toString();
  }
}
