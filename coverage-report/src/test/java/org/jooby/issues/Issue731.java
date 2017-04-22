package org.jooby.issues;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue731 extends ServerFeature {

  {

    executor("worker1", Executors.newSingleThreadExecutor());

    get("/731", deferred("worker1", req -> req.body().toOptional().orElse("nobody")));

    post("/731", deferred("worker1", req -> req.body(String.class)));

    post("/731/files",
        deferred("worker1", req -> Files.readAllBytes(req.file("file").file().toPath())));

  }

  @Test
  public void appShouldBeAbleToReadTheRequestBodyWhenDeferred() throws Exception {
    request()
        .post("/731")
        .body("HelloWorld!", "text/plain")
        .expect(200)
        .expect("HelloWorld!");
  }

  @Test
  public void appShouldBeAbleToReadTheFileWhenDeferred() throws Exception {
    request()
        .post("/731/files")
        .multipart()
        .file("file", "HelloWorld!".getBytes(StandardCharsets.UTF_8), "text/plain", "731.txt")
        .expect(200)
        .expect("HelloWorld!");
  }

  @Test
  public void getShouldNotBeAffected() throws Exception {
    request()
        .get("/731")
        .expect(200)
        .expect("nobody");
  }
}
