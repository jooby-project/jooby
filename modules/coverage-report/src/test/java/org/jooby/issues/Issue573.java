package org.jooby.issues;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.jooby.guava.GuavaCache;
import org.jooby.guava.GuavaSessionStore;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.cache.Cache;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue573 extends ServerFeature {

  @Path("/573")
  public static class MvcRoute {

    private Cache<String, Object> cache;

    @Inject
    public MvcRoute(final Cache<String, Object> cache) {
      this.cache = cache;
    }

    @GET
    public Object get() throws ExecutionException {
      return cache.get("foo", () -> "bar");
    }
  }

  {
    use(ConfigFactory.empty()
        .withValue("guava.cache", ConfigValueFactory.fromAnyRef("maximumSize=10"))
        .withValue("guava.session", ConfigValueFactory.fromAnyRef("maximumSize=10")));

    use(GuavaCache.newCache());

    session(GuavaSessionStore.class);

    use(MvcRoute.class);
  }

  @Test
  public void shouldInjectCacheIntoMvcRoute() throws Exception {
    request()
        .get("/573")
        .expect("bar");
  }

}
