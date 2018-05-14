package org.jooby;

import java.util.Optional;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class EmptyUploadsOnGetRequestFeature extends ServerFeature {

  {
    get("/file", req -> {
      Optional<Upload> upload = req.param("file").toOptional(Upload.class);
      return upload;
    });

    post("/file", req -> {
      Optional<Upload> upload = req.param("file").toOptional(Upload.class);
      return upload;
    });

  }

  @Test
  public void nouploads() throws Exception {
    request()
        .get("/file")
        .expect("Optional.empty");

    request()
        .post("/file")
        .multipart()
        .file("upload", "<p></p>".getBytes(), "application/xml", "html.xml")
        .expect("Optional.empty");
  }

}
