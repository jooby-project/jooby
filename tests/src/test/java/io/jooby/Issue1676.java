package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MultipartBody;

import java.util.stream.Collectors;

import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1676 {
  @ServerTest
  public void shouldParseFileKeyFromMultipartUpload(ServerTestRunner runner) {
    String content = "Proin pulvinar purus ac lacus pharetra, tristique fringilla lacus finibus. "
        + "Praesent bibendum tellus feugiat euismod sollicitudin. Integer sed sem vestibulum sem "
        + "imperdiet blandit. Vivamus in lorem quis orci gravida hendrerit.";
    runner.define(app -> {
      app.post("/1676", ctx ->
          ctx.files().stream()
              .collect(Collectors.toMap(FileUpload::getName, FileUpload::getFileName))
      );
    }).ready(client -> {
      client.post("/1676", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("filekey", "19kb.txt",
              create(content, okhttp3.MediaType.parse("text/plain")))
          .build(), rsp -> {
        assertEquals("{filekey=19kb.txt}", rsp.body().string());
      });
    });
  }
}
