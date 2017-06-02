package org.jooby.ftl;

import static org.junit.Assert.assertSame;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;

public class FtlNoCacheFeature extends ServerFeature {

  {
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
