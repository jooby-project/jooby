package org.jooby.issues;

import java.io.ByteArrayInputStream;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue169 extends ServerFeature {

  {

    use (new Jackson());

    get("/file", (req, rsp)-> {
      rsp.download("file.txt", new ByteArrayInputStream("hello".getBytes()));
    });

  }

  @Test
  public void shouldDownloadAFile() throws Exception {
    request()
      .get("/file")
      .expect(200)
      .header("Content-Length", 5)
      .header("Content-Disposition", "attachment; filename=\"file.txt\"; filename*=utf-8''file.txt");
  }
}
