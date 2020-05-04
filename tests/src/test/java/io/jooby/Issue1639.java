package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1639 {

  @ServerTest
  public void shouldNotAccessToClassFromFileSystemAsset(ServerTestRunner runner) {
    runner.define(app -> {
      app.assets("/static/?*", userdir("src", "test", "resources", "static"));
    }).ready(client -> {
      client.get("/static/js/index.js", rsp -> {
        assertEquals("(function () { console.log('index.js');});", rsp.body().string().trim());
      });

      client.get("/static/io/jooby/Issue1639.js.class", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/static/io/jooby/Issue1639.class", rsp -> {
        assertEquals(404, rsp.code());
      });
    });
  }

  @ServerTest
  public void shouldNotAccessToClassFromCpAssetSource(ServerTestRunner runner) {
    runner.define(app -> {
      app.assets("/static/?*", "/static");
    }).ready(client -> {
      client.get("/static/js/index.js", rsp -> {
        assertEquals("(function () { console.log('index.js');});", rsp.body().string().trim());
      });

      client.get("/static/../io/jooby/Issue1639.class", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/static/..%252fio/jooby/Issue1639.class", rsp -> {
        assertEquals(404, rsp.code());
      });
    });
  }

  private static Path userdir(String... segments) {
    Path path = Paths.get(System.getProperty("user.dir"));
    for (String segment : segments) {
      path = path.resolve(segment);
    }
    return path;
  }
}
