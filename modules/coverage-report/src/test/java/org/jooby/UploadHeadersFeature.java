package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class UploadHeadersFeature extends ServerFeature {

  {
    post("/file", req -> {
      Upload upload = req.file("file");
      String encoding = upload.header("Content-Transfer-Encoding").value();
      String type = upload.header("Content-Type").value();
      String disposition = upload.header("Content-Disposition").value();
      return encoding + ";" + type + ";" + disposition + ";"
          + upload.header("noHeader").toOptional(String.class);
    });
  }

  @Test
  public void headers() throws Exception {
    request()
        .post("/file")
        .multipart()
        .file("file", "<project></project>".getBytes(), "application/xml;charset=UTF-8", "pom.xml")
        .expect(
            "binary;application/xml; charset=UTF-8;form-data; name=\"file\"; filename=\"pom.xml\";Optional.empty");

  }

}
