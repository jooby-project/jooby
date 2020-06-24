package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.MultipartBody;

import java.nio.file.Path;
import java.nio.file.Paths;

import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1815 {

  @ServerTest
  public void shouldNotThrowClassCastExceptionOnFileUploadWithCsrfHandler(ServerTestRunner runner) {
    runner.define(app -> {
      app.before(new CsrfHandler());

      app.post("/1815", ctx -> ctx.multipart("file"));
    }).ready(http -> {
      http.post("/1815", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("file", "fileupload.js",
              create(userdir("src", "test", "resources", "files", "fileupload.js").toFile(),
                  MediaType.parse("application/javascript")))
          .build(), rsp -> {
        assertEquals(StatusCode.FORBIDDEN_CODE, rsp.code());
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
