package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class AssetETagFeature extends ServerFeature {

  {
    assets("/assets/**");
  }

  @Test
  public void etag() throws Exception {
    request()
        .get("/assets/file.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Content-Length", 15)
        .header("ETag", etag -> {
          request()
              .get("/assets/file.js")
              .header("If-None-Match", etag)
              .expect(304)
              .header("ETag", etag)
              .empty();
        });
  }

}
