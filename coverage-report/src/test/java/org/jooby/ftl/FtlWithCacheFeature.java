package org.jooby.ftl;

import static org.junit.Assert.assertTrue;

import org.jooby.internal.ftl.GuavaCacheStorage;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import freemarker.template.Configuration;

public class FtlWithCacheFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.env", ConfigValueFactory.fromAnyRef("prod")));

    use(new Ftl());

    get("/", req -> {
      assertTrue(req.require(Configuration.class).getCacheStorage() instanceof GuavaCacheStorage);
      return "guava";
    });
  }

  @Test
  public void hbs() throws Exception {
    request()
        .get("/")
        .expect("guava");
  }

}
