package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class AssetFeature extends ServerFeature {

  {
    assets("/assets/**");
  }

  @Test
  public void jsAsset() throws Exception {
    request()
        .get("/assets/file.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Last-Modified", lastModified -> {
          request()
              .get("/assets/file.js")
              .header("If-Modified-Since", lastModified)
              .expect(304)
              .empty();
        });
  }

  @Test
  public void cssAsset() throws Exception {
    request()
        .get("/assets/file.css")
        .expect(200)
        .header("Content-Type", "text/css;charset=UTF-8")
        .header("Last-Modified", lastModified -> {
          request()
              .get("/assets/file.js")
              .header("If-Modified-Since", lastModified)
              .expect(304)
              .empty();
        });
  }

  @Test
  public void imageAsset() throws Exception {
    request()
        .get("/assets/favicon.ico")
        .expect(200)
        .header("Content-Type", "image/x-icon")
        .header("Last-Modified", lastModified -> {
          request()
              .get("/assets/favicon.ico")
              .header("If-Modified-Since", lastModified)
              .expect(304)
              .empty();
        });
  }

}
