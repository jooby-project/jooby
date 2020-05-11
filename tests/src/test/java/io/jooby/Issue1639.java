package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import org.apache.http.client.fluent.Request;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1639 {

  @ServerTest
  public void shouldNotAccessToClassFromFileSystemAsset(ServerTestRunner runner) {
    runner.define(app -> {
      app.assets("/static/?*", userdir("src", "test", "resources", "static"));
    }).ready(client -> {
      client.get("/static/..%252ffiles/fileupload.js", rsp -> {
        assertEquals(404, rsp.code());
      });
      client.get("/static/../files/fileupload.js", rsp -> {
        assertEquals(404, rsp.code());
      });
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
      client.get("/static/foo/../js/index.js", rsp -> {
        assertEquals("(function () { console.log('index.js');});", rsp.body().string().trim());
      });
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

  @ServerTest
  public void shouldNotAccessToClassFromCpAssetSourceWithDifferentClient(ServerTestRunner runner) {
    runner.define(app -> {
      app.assets("/static/?*", "/static");
    }).ready(client -> {
      assertEquals(404,
          Request
              .Get("http://localhost:" + client.getPort() + "/static/../io/jooby/Issue1639.class")
              .execute()
              .returnResponse()
              .getStatusLine().getStatusCode());

      assertEquals("(function () { console.log('index.js');});",
          Request.Get("http://localhost:" + client.getPort() + "/static/foo/../js/index.js")
              .execute()
              .returnContent()
              .asString().trim());

      assertEquals("(function () { console.log('index.js');});",
          Request.Get("http://localhost:" + client.getPort() + "/static/js/index.js")
              .execute()
              .returnContent()
              .asString().trim());

      assertEquals(404,
          Request.Get(
              "http://localhost:" + client.getPort() + "/static/..%252fio/jooby/Issue1639.class")
              .execute()
              .returnResponse()
              .getStatusLine().getStatusCode());
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
