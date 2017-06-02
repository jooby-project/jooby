package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class CdnAssetFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("assets.cdn",
            ConfigValueFactory.fromAnyRef("http://d7471vfo50fqt.cloudfront.net")));

    assets("/assets/**");
  }

  @Test
  public void cdn() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/assets/file.js")
        .expect(302)
        .header("Location", "http://d7471vfo50fqt.cloudfront.net/assets/file.js");
  }

}
