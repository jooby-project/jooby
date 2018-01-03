package org.jooby.issues;

import org.jooby.Upload;
import org.jooby.pac4j.Auth;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue361 extends ServerFeature {

  {
    use(new Auth().basic());

    get("/", () -> "OK");

    post("/api/protected/upload", req -> {
      Upload file = req.file("myfile");
      return file.name();
    });
  }

  @Test
  public void shouldPac4jAuthContextShouldNotFailWithFileUploads() throws Exception {
    request()
        .get("/auth?username=test&password=test")
        .expect("OK");

    request()
        .post("/api/protected/upload")
        .multipart()
        .add("username", "test")
        .add("password", "test")
        .file("myfile", "<xml></xml>".getBytes(), "application/xml", "pom.xml")
        .expect(200);
  }

}
