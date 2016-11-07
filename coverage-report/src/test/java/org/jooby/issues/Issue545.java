package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue545 extends ServerFeature {

  {
    assets("/assets/video.mp4");
  }

  @Test
  public void shouldGetMp4Mimetype() throws Exception {
    request()
        .get("/assets/video.mp4")
        .expect(200)
        .header("Content-Length", "383631")
        .header("Content-Type", "video/mp4");
  }

}
