package org.jooby.hbs;

import static org.junit.Assert.assertTrue;

import org.jooby.hbs.Hbs;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbsWithCacheFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.env", ConfigValueFactory.fromAnyRef("prod")));

    use(new Hbs());

    get("/", req -> {
      assertTrue(req.require(Handlebars.class).getCache() instanceof GuavaTemplateCache);
      return "guava";
    });
  }

  @Test
  public void hbs() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("guava");
  }

}
