package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class AssetLastModifiedOffFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("assets.etag", ConfigValueFactory.fromAnyRef(false))
        .withValue("assets.lastModified", ConfigValueFactory.fromAnyRef(false)));
    assets("/assets/**");
  }

  @Test
  public void lastModifiedOff() throws Exception {
    request()
        .get("/assets/file.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Content-Length", 15)
        .header("ETag", (String) null)
        .header("Last Modified", (String) null);
  }

}
