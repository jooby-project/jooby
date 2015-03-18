package org.jooby.ftl;

import static org.junit.Assert.assertSame;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;

public class FtlCacheOffFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.env", ConfigValueFactory.fromAnyRef("prod"))
        .withValue("freemarker.cache", ConfigValueFactory.fromAnyRef("")));

    use(new Ftl());

    get("/", req -> {
      assertSame(NullCacheStorage.INSTANCE, req.require(Configuration.class).getCacheStorage());
      return "noop";
    });
  }

  @Test
  public void ftl() throws Exception {
    request()
        .get("/")
        .expect("noop");
  }

}
