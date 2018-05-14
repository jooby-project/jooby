package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class AssetETagOffFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("assets.etag", ConfigValueFactory.fromAnyRef(false)));
    assets("/assets/**");
  }

  @Test
  public void etag() throws Exception {
    request()
        .get("/assets/file.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Content-Length", 15)
        .header("ETag", (String) null);
  }

}
